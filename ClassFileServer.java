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

    private String docroot;

    private static int DefaultServerPort = 2001;

    /**
     * Constructs a ClassFileServer.
     *
     * @param path the path where the server locates files
     */
    public ClassFileServer(ServerSocket ss, String docroot) throws IOException
    {
        super(ss);
        this.docroot = docroot;
    }

    /**
     * Returns an array of bytes containing the bytes for
     * the file represented by the argument <b>path</b>.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file corresponding
     * to <b>path</b> could not be loaded.
     */
    public byte[] getBytes(String path) // Read a certain file from the docroot (to send to the client)
        throws IOException
    {
        System.out.println("reading: " + path);
        File f = new File(docroot + File.separator + path);
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

    /**
     * Main method to create the class server that reads
     * files. This takes two command line arguments, the
     * port on which the server accepts requests and the
     * root of the path. To start up the server: <br><br>
     *
     * <code>   java ClassFileServer <port> <path>
     * </code><br><br>
     *
     * <code>   new ClassFileServer(port, docroot);
     * </code>
     */
    public static void main(String args[]) {
        Scanner cin = new Scanner(System.in);
        // "Start as: java ClassFileServer port docroot TLS"
        System.out.println("");
        System.out.println("[Server Setup] Initializing Lupu List Server..");
        System.out.println("");
        int port = DefaultServerPort;
        String docroot = "";
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            docroot = args[1];
        }
        String type = "TLS"; // Make sure this is "TLS"
        try {
            System.out.println("[Server Setup] Setting up Secure Socket..");
            ServerSocketFactory ssf = ClassFileServer.getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port); // create serverSocket with the specifications from ssf

            // When you create this Object, the Super (ClassServer) will handle the rest (multithreading and whatnot)
            System.out.println("[Server Setup] Successfully Setup");
            new ClassFileServer(ss, docroot);

        } catch (IOException e) {
            System.out.println("[Server Setup] Unable to start ClassServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            String command = cin.nextLine();
            while (!command.equals("stop")) {
                System.out.println("Unrecognized command '" + command + "'. Type 'stop' to terminate server");
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
}