// Holden Ernest - 6/29/2024
// Based off Oracles Docs
// https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm#JSSEC-GUID-3561ED02-174C-4E65-8BB1-5995E9B7282C

/* 
 * This file takes in a Server socket then accepts connections on it.
 * On a connection it will parse the recieved information to determine
 * what actions the server needs to take.
 * 
 * Read more about the Project:
 * https://github.com/HoldenErnest/ListServer
 * 
 * The 3 interactions that can occur:
 * Save - save a list to the server
 * Load - load a list from the server
 * Update - change perms of a list
 */

import java.io.*;
import java.net.*;
import javax.net.*;
import java.util.Date;

public abstract class ClassServer implements Runnable {

    private ServerSocket server = null;
    private BufferedWriter log;

    protected ClassServer(ServerSocket ss) // this is only called once to accept the first connection
    {
        server = ss;
        
        try {
            log = new BufferedWriter(new FileWriter("log", true));
        } catch (Exception e) {
            System.out.println("Problem Setting up log file");
        }

        newListener();
        System.out.println("[LIVE] Awaiting Connection..");
        
    }

    public abstract byte[] getBytes(String path) throws IOException, FileNotFoundException;

    // Event on new thread creation
    public void run()
    {
        Socket socket;
        InetAddress srcAddr;
        int srcPort;

        // accept a connection
        try {
            socket = server.accept(); // wait for a connection
            srcAddr = socket.getInetAddress();
            srcPort = socket.getLocalPort();


            log("Connection: " + srcAddr + ":" + srcPort);
        } catch (IOException e) {
            System.out.println("Server died: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // create a new thread to wait for if there are any more connections
        newListener();

        try {
            OutputStream rawOut = socket.getOutputStream();

            PrintWriter out = new PrintWriter(
                                new BufferedWriter(
                                new OutputStreamWriter(
                                rawOut)));
            try {
                // get path to class file from header
                BufferedReader in =
                    new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String path = getPath(in);
                // retrieve data from the file
                byte[] data = getBytes(path);
                // send bytecodes in response (assumes HTTP/1.0 or later)
                try {
                    out.print("HTTP/1.0 200 OK\r\n");
                    out.print("Content-Length: " + data.length +
                                   "\r\n");
                    out.print("Content-Type: text/html\r\n\r\n");
                    out.flush();
                    rawOut.write(data);
                    rawOut.flush();
                } catch (IOException ie) {
                    ie.printStackTrace();
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
                // write out error response
                out.println("HTTP/1.0 400 " + e.getMessage() + "\r\n");
                out.println("Content-Type: text/html\r\n\r\n");
                out.flush();
            }

        } catch (IOException ex) {
            // eat exception (could log error to log file, but
            // write out to stdout for now).
            System.out.println("error writing response: " + ex.getMessage());
            ex.printStackTrace();

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                
            }
        }
    }

    /**
     * Create a new thread to listen.
     */
    private void newListener()
    {
        (new Thread(this)).start(); // All new threads start at the run() method where they will patiently wait for a new client connection
    }

    /**
     * Returns the path to the file obtained from
     * parsing the HTML header.
     */
    private static String getPath(BufferedReader in) throws IOException {
        String line = in.readLine();
        System.out.println("read line: " + line);
        String path = "";
        // extract class from GET line
        if (line.startsWith("GET /")) {
            line = line.substring(5, line.length()-1).trim();
            int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
        }

        // eat the rest of header
        do {
            line = in.readLine();
            System.out.println("read line: " + line);
        } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        if (path.length() != 0) {
            return path;
        } else {
            throw new IOException("Malformed Header");
        }
    }
    
    private void log(String s) {
        try {
            Date date = new Date();
            log.append("\r\n");
            log.append("["+ date + "] ");
            log.append(s);
            // lock
            log.flush();
            // unlock
        } catch (Exception e) {
            System.out.println("problem Logging: " + e);
        }
    }

}