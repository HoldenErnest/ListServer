// Holden Ernest - 6/29/2024
// Based off Oracles Docs
// https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm#JSSEC-GUID-3561ED02-174C-4E65-8BB1-5995E9B7282C

/* 
 * This file starts up a secure socket then forwards it to ClassServer.java to handle the connections
 */

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.util.Scanner;

public class ClassFileServer extends ClassServer {

    private static String docroot;
    private static final String userroot = "users";
    private static final String listroot = "lists";

    private static int DefaultServerPort = 2001;

    private static Scanner cin;

    /**
     * Constructs a ClassFileServer.
     *
     * @param path the path where the server locates files
     */
    public ClassFileServer(ServerSocket ss, String docroot) throws IOException
    {
        super(ss);
        ClassFileServer.docroot = docroot;
    }

    /**
     * Returns an array of bytes containing the bytes for
     * the file represented by the argument <b>path</b>.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file corresponding
     * to <b>path</b> could not be loaded.
     */

    public static byte[] getList(User user, String listName) throws IOException {
        if (listName.contains(File.separator)) throw new IOException("Filename cannot contain sub directories");
        return getBytes("lists" + File.separator + listName);
    }

    private static byte[] getBytes(String path) throws IOException { // Read a certain file from the docroot
        
        System.out.println("reading: " + path);
        File f = new File(ClassFileServer.docroot + File.separator + path);
        int length = (int)(f.length());
        if (length == 0) {
            throw new IOException("File length is zero: " + path);
        } else {
            FileInputStream fin = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fin);

            byte[] bytecodes = new byte[length];
            in.readFully(bytecodes);
            return bytecodes;
        }
    }
    public static void main(String args[]) {
        cin = new Scanner(System.in);
        // "Start as: java ClassFileServer port docroot TLS"
        System.out.println("");
        System.out.println("[Server Setup] Initializing Lupu List Server..");
        System.out.println("");
        int port = DefaultServerPort;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            ClassFileServer.docroot = args[1];
        }
        String type = "TLS"; // Make sure this is "TLS"
        try {
            System.out.println("[Server Setup] Setting up Secure Socket..");
            ServerSocketFactory ssf = ClassFileServer.getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port); // create serverSocket with the specifications from ssf

            // When you create this Object, the Super (ClassServer) will handle the rest (multithreading and whatnot)
            System.out.println("[Server Setup] Successfully Setup");
            new ClassFileServer(ss, ClassFileServer.docroot);

        } catch (IOException e) {
            System.out.println("[Server Setup] Unable to start ClassServer: " + e.getMessage());
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
                    System.out.println("User \'" + user + "\' found!");
            } else {
                System.out.println("User \'" + user + "\' not found.");
            }
        } else { // password given
            if (UserDB.hasUser(user, pass)) {
                System.out.println("User \'" + user + "\' found!");
            } else {
                System.out.println("User \'" + user + "\' not found with that password.");
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
        return ClassFileServer.docroot + ClassFileServer.userroot + File.separator;
    }
    public static String getListsPath() {
        return ClassFileServer.docroot + ClassFileServer.listroot + File.separator;
    }
}