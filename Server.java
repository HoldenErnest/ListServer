// Holden Ernest - 6/29/2024
// Based off Oracles Docs
// https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm#JSSEC-GUID-3561ED02-174C-4E65-8BB1-5995E9B7282C

/* 
 * This file starts up a secure socket then forwards it to ClientConnection.java to handle the connections
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.util.Scanner;

public class Server extends ClientConnection {

    private static String docroot;
    private static final String userroot = "users";
    private static final String listroot = "lists";

    private static int DefaultServerPort = 2001;

    private static Scanner cin;

    /**
     * Constructs a Server.
     *
     * @param path the path where the server locates files
     */
    public Server(ServerSocket ss, String docroot) throws IOException
    {
        super(ss);
        Server.docroot = docroot;
    }

    /**
     * Returns an array of bytes containing the bytes for
     * the file represented by the argument <b>path</b>.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file corresponding
     * to <b>path</b> could not be loaded.
     */

    public static byte[] getList(ConnectionParser connection, Response res) throws Exception { // listPath should be "username/alist.csv"
        String username = connection.getUsername();
        String listPath = connection.getListPath();
        int version = connection.getListVersion();
        int bversion = connection.getListBaseVersion();

        ListMetaParser listMeta = getListMeta(listPath);
        if (listMeta == null) throw new IOException("NO METADATA FOUND");
        if (!listPath.startsWith(username)) { // this list is from another user..
            if (listMeta == null || !listMeta.hasReadAccess(username)) throw new IOException("Not allowed read access to this list");
        }
        if (listMeta.olderThanSameAs(version)) { // the clients files is more recent than the remote file
            throw new IOException("Local File is Newer");
        }
        res.setVersion(listMeta.getVersion());
        return getBytes(Server.getListsPath() + listPath);
    }
    private static ListMetaParser getListMeta(String listPath) throws IOException { // This takes username/listname
        return new ListMetaParser(getBytes(Server.getListsPath() + listPath + ".meta"));
    }
    public static void saveList(ConnectionParser cp, Response res) throws Exception {
        String username = cp.getUsername();
        String listPath = cp.getListPath();
        byte[] listBytes = cp.getData();
        int baseVersion = cp.getListBaseVersion();
        ListMetaParser listMeta = getListMeta(listPath);
        if (!listPath.startsWith(username)) { // this list is from another user..
            if (listMeta == null || !listMeta.hasWriteAccess(username)) throw new IOException("Not allowed write access to this list");
        }
        if (listMeta.sameVersion(baseVersion)) { // only write if its a newer version
            writeListToFile(username, listPath, listBytes, true);
            writeMetaFile(listMeta, listPath);
            res.setStatus(200);
        } else { // the file attempting to get saved is based off a different list
            System.out.println("CONFLICTING SAVE");
            res.setStatus(300);
            String data = new String(getList(cp, res), StandardCharsets.UTF_8);
            res.setData(data);
        }
    }


    private static void writeListToFile(String username, String listPath, byte[] listByteString, Boolean overwrite) throws IOException { // Use saveList(), dont call this directly (for user checking reasons)
        File f = new File(Server.getListsPath() + listPath);
        int length = (int)(f.length());
        if (length == 0 || overwrite) { // if it doesnt exist or you can overwrite it, start writing
            System.out.println("[FILE] Writing to file: " + Server.getListsPath() + listPath);
            FileOutputStream outputStream = new FileOutputStream(f);
            outputStream.write(listByteString);
            outputStream.close();

            if (length == 0) { // length was 0, meaning it was a newly created list file
                newMetaFile(username, listPath, false);
            }

        } else {
            throw new IOException("[FILE] Cannot overwrite: " + Server.getListsPath() + listPath);
        }
    }
    private static void newMetaFile(String username, String listPath, Boolean overwrite) throws IOException { // only to be called when youre creating new lists (writeListToFile())
        System.out.println("Writing new Metafile for: " + listPath);
        String metafile = "owner: " + username + "\nread: " + "\nwrite: " + "\nversion: -1";
        File f = new File(Server.getListsPath() + listPath + ".meta");
        int length = (int)(f.length());
        if (length == 0 || overwrite) {
            FileOutputStream outputStream = new FileOutputStream(f);
            
            outputStream.write(metafile.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        }
    }
    private static void writeMetaFile(ListMetaParser lmp, String listPath) throws IOException {
        System.out.println("SAVE META: " + Server.getListsPath() + listPath + ".meta");
        File f = new File(Server.getListsPath() + listPath + ".meta");
        FileOutputStream outputStream = new FileOutputStream(f);
        outputStream.write(lmp.getBytes());
        outputStream.close();
    }
    private static byte[] getBytes(String path) throws IOException { // Read a certain file from the docroot // this takes /home/absolute/path/to/file
        
        System.out.println("[FILE] Loading File: " + path);
        File f = new File(path);
        int length = (int)(f.length());
        if (length == 0) {
            throw new IOException("File doesnt exist");
        } else {
            DataInputStream in = new DataInputStream(new FileInputStream(f));

            byte[] bytecodes = new byte[length];
            in.readFully(bytecodes);
            in.close();
            return bytecodes;
        }
    }
    public static void main(String args[]) {
        cin = new Scanner(System.in);
        // "Start as: java Server port docroot TLS"
        System.out.println("");
        System.out.println("[Server Setup] Initializing Lupu List Server..");
        System.out.println("");
        int port = DefaultServerPort;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            Server.docroot = args[1];
        }
        String type = "TLS"; // Make sure this is "TLS"
        try {
            System.out.println("[Server Setup] Setting up Secure Socket..");
            ServerSocketFactory ssf = Server.getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port); // create serverSocket with the specifications from ssf

            // When you create this Object, the Super (ClientConnection) will handle the rest (multithreading and whatnot)
            System.out.println("[Server Setup] Successfully Setup");
            new Server(ss, Server.docroot);

        } catch (IOException e) {
            System.out.println("[Server Setup] Unable to start ClientConnection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            String command = cin.nextLine();
            while (!command.equals("stop")) {
                switch (command) {
                    case "user add":
                        tryMakeUser();
                        break;
                    case "user get":
                        tryGetUser();
                        break;
                    default:
                        System.out.println("Unrecognized command '" + command + "'. Type 'help' see all commands or 'stop' to terminate server");
                        break;
                }

                command = cin.nextLine();
            }
            // STOP THE SERVER
            System.out.println("[Server Shutdown] Shutting down server..");
            System.exit(0);
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try {
                // set up key manager to do server authentication
                SSLContext ctx;
                KeyManagerFactory kmf;
                KeyStore ks;
                char[] passphrase = "anewkey".toCharArray(); // password for the Java Keystore (not that important)

                ctx = SSLContext.getInstance("TLS");
                kmf = KeyManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS"); // make a keystore of type .jks

                ks.load(new FileInputStream("cert/server.keystore"), passphrase); // load the keystore from a file, a password is needed to use it
                kmf.init(ks, passphrase);
                ctx.init(kmf.getKeyManagers(), null, null);

                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // attempt to do ssl socket, otherwise just do default
            System.out.println("PROBLEM: not SSL socket");
            return ServerSocketFactory.getDefault();
        }
        return null;
    }

    private static void tryMakeUser() {
        System.out.print("Enter: <Username> <Password>: ");
        String userPass = cin.nextLine();
        if (assertBadEntry(userPass.length() == 0)) return;
        int sp = userPass.indexOf(" ");
        if (assertBadEntry(sp == -1)) return;

        String user = userPass.substring(0,sp);
        char[] pass = userPass.substring(sp+1).toCharArray();
        userPass = null;
        if (assertBadEntry(pass.length == 0 || user.length() == 0)) return;
        UserDB.createUser(user, pass);
        System.out.println("User \'" + user + "\' added!\n");
    }
    private static void tryGetUser() { // see if there is a user in the db, password check is optional
        System.out.print("Enter: <Username> <Password?>: ");
        String userPass = cin.nextLine();
        if (assertBadEntry(userPass.length() == 0)) return;
        char[] pass = null;
        String user;

        int sp = userPass.indexOf(" ");
        if (sp == -1) {
            sp = userPass.length();
        } else {
            pass = userPass.substring(sp+1).toCharArray();
        }
        user = userPass.substring(0,sp);
        userPass = null;
        if (assertBadEntry(user.length() == 0)) return;

        if (pass == null) { // no password given
            if (UserDB.hasUser(user)) {
                    System.out.println("User \'" + user + "\' found!\n");
            } else {
                System.out.println("User \'" + user + "\' not found.\n");
            }
        } else { // password given
            if (UserDB.hasUser(user, pass)) {
                System.out.println("User \'" + user + "\' found!\n");
            } else {
                System.out.println("User \'" + user + "\' not found with that password.\n");
            }
            
        }
    }

    private static Boolean assertBadEntry(Boolean b) {
        if (b) {
            System.out.println("Invalid Entry.");
            return true;
        }
        return false;
    }


    // Accessor
    public static String getUsersPath() {
        return Server.docroot + Server.userroot + File.separator;
    }
    public static String getListsPath() {
        return Server.docroot + Server.listroot + File.separator;
    }
}