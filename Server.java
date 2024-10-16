// Holden Ernest - 6/29/2024
// Based off Oracles Docs
// https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm#JSSEC-GUID-3561ED02-174C-4E65-8BB1-5995E9B7282C

/* 
 * This file starts up a secure socket then forwards it to ClientConnection.java to handle the connections
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static byte[] getList(ConnectionParser connection, Response res, Boolean versionCare) throws Exception { // listPath should be "username/alist.csv"
        String username = connection.getUsername();
        String listPath = connection.getListPath();
        int version = connection.getListVersion();

        ListMetaParser listMeta = getListMeta(listPath);
        if (listMeta == null) throw new IOException("NO METADATA FOUND");
        if (!listMeta.isOwner(username)) { // this list is from another user..
            if (!listMeta.hasReadAccess(username)) throw new IOException("Not allowed read access to this list");
        }
        if (listMeta.olderThanSameAs(version) && versionCare) { // the clients files is more recent than the remote file
            System.out.println("cant send: " + listMeta.getVersion() + " <= " + version);
            throw new IOException("Local File is Newer");
        } 
        res.setVersion(listMeta.getVersion());
        return getBytes(Server.getListsPath() + listPath);
    }
    public static void saveList(ConnectionParser cp, Response res) throws Exception {
        String username = cp.getUsername();
        String listPath = cp.getListPath();
        byte[] listBytes = cp.getData();
        int version = cp.getListVersion();
        ListMetaParser listMeta = null;
        try {
            listMeta = getListMeta(listPath);
        } catch(Exception e) {}

        if (listMeta == null) { // if there is no metafile that means you need to make a new one
            newMetaFile(username, listPath, true);
            listMeta = getListMeta(listPath);
        }

        if (!listMeta.isOwner(username)) { // this list is from another user..
            if (!listMeta.hasWriteAccess(username)) {
                res.setStatus(301);
                String data = new String(getList(cp, res,false), StandardCharsets.UTF_8); // attempt to read the list instead, if no permissions THEN it will error 400
                res.setData(data);
                return;
            }
        }
        
        if (listMeta == null || listMeta.sameVersion(version)) { // only write if its a newer version OR this is a new file
            writeListToFile(username, listPath, listBytes, true);
            listMeta.setVersion(version+1);
            writeMetaFile(listMeta, listPath);
            res.setVersion(version+1); // Successful save, increase its version by 1
            res.setStatus(200);
            res.setData("Successfully Saved List: " + listPath);
        } else { // the file attempting to get saved is based off a different list
            System.out.println("CONFLICTING SAVE, sending old list data");
            res.setStatus(300);
            String data = "";
            data = new String(getList(cp, res,false), StandardCharsets.UTF_8);
            res.setData(data);
            res.setVersion(version);
        }
    }
    public static void getAllLists(ConnectionParser cp, Response res) throws Exception {
        File folder = new File(Server.getListsPath() + cp.getUsername());
        String allFiles;
        allFiles = listAllCSVInFolder(folder); // can throw exception but really it shouldnt, either theres nothing in these folders = "" or there are things in that folder -_-
        res.setData(allFiles);
    }

    private static String listAllCSVInFolder(File folder) throws Exception {
        String allFiles = "";
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.getName().endsWith(".csv")) {
                allFiles += fileEntry.getName().substring(0,fileEntry.getName().length()-4) + " ";
            }
        }
        return allFiles;
    }
    private static ListMetaParser getListMeta(String listPath) throws IOException { // This takes username/listname
        return new ListMetaParser(getBytes(Server.getListsPath() + listPath + ".meta"));
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
        String metafile = "owner: " + username + "\nread: " + "\nwrite: " + "\nversion: 0";
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
                    case "permissions":
                        permSetCLI();
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

    public static void permissionSet(String listPath, String user, int rw) { // create a hardlink for a user to another users list and metadata. (rw: 2=r/w, 1=r, 0=none) EX: "user/list user2 2"
        //make sure the list exists
        ListMetaParser lmp = null;
        try {
            lmp = getListMeta(listPath);
        } catch (Exception e) {
            System.out.println("list does not exist");
            return;
        }
        Boolean hadChange = false;
        //edit the metadata there
        if (rw == 2) {
            hadChange = lmp.tryAllowWrite(user);
        } else if (rw == 1) {
            hadChange = lmp.tryAllowRead(user, false);
        } else {
            hadChange = lmp.removePermissions(user);
            removeHardLink(listPath, user);
        }
        try {
            if (hadChange)
                writeMetaFile(lmp, listPath);
        } catch (Exception e) {
            System.out.println("Problem writing metafile");
            return;
        }
        //make/remove hardlink if neccessary on the user side
        createHardLink(listPath, user);
    }
    private static void permSetCLI() { // CLI
        System.out.print("Enter: <Username>/<Listname> <Username> <2/1/0(rw/r/-)>: ");
        String input = cin.nextLine();
        if (assertBadEntry(input.length() == 0)) return;
        int sp = input.indexOf(" ");
        int sp2 = input.lastIndexOf(" ");
        if (assertBadEntry(sp == -1 || sp2 == sp)) return;
        String listPath = input.substring(0,sp);
        String user = input.substring(sp+1,sp2);
        int rw = Integer.parseInt(input.substring(sp2+1));

        permissionSet(listPath+".csv", user, rw);
    }

    private static void createHardLink(String listPath, String user) { // link from a list to another users directory
        try {
            String path1 = getListsPath() + File.separator + listPath;
            String path2 = getListsPath() + File.separator + user + File.separator + listPath.replaceAll("/", ".");
            Path targetFile = Paths.get(path1);
            Path linkPath = Paths.get(path2);
            if (Files.exists(linkPath)) return; // if this already exists, dont overwrite anything. THIS COULD BE A NON-LINK(user owns the list), WHICH IS FINE
            Files.createLink(linkPath, targetFile);
            targetFile = Paths.get(path1 + ".meta");
            linkPath = Paths.get(path2 + ".meta");
            if (Files.exists(linkPath)) Files.delete(linkPath); // if the list link didnt exist but the meta does, remove this meta (it could have the incorrect permissions)
            Files.createLink(linkPath, targetFile);
            System.out.println("Hard link created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void removeHardLink(String listPath, String user) {
        String path = getListsPath() + File.separator + user + File.separator + listPath.replaceAll("/", ".");
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("Hard link removed successfully.");
            } else {
                System.out.println("Failed to remove hard link.");
            }
        } else {
            System.out.println("File does not exist.");
        }
        file = new File(path + ".meta");
        if (file.exists()) {
            file.delete();
        }
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