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

    // Event on new a thread starting up
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

            log("New Connection: " + srcAddr + ":" + srcPort);
        } catch (IOException e) {
            System.out.println("Server died: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // create a new thread to wait for if there are any more connections
        newListener();

        try {
            OutputStream rawOut = socket.getOutputStream(); // This output is used to directly send bytes (so use for sending file data)
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(rawOut))); // this output is used to send text only

            try {
                // get path to class file from header
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Parse the input header for info
                String path = getPath(in);
                try {
                    byte[] data = getBytes(path);

                    writeHeaderToSocket(out, data.length);
                    writeTextToSocket(out, "testing");
                    writeBytesToSocket(rawOut,data);

                } catch (Exception ie) {
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
            System.out.println("error writing response: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                System.out.println("Closing connection on a thread");
                // TODO: log this closing connection
                socket.close();
                
            } catch (IOException e) {
                
            }
        }
    }

    // Functions to Write to Socket ----------------------------
    private void writeHeaderToSocket(PrintWriter out, int cl) {
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Content-Length: " + cl + "\r\n");
        out.print("Content-Type: text/html\r\n\r\n");
        out.flush();
    }
    private void writeTextToSocket(PrintWriter out, String s) {
        out.print(s);
        out.flush();
    }
    private void writeBytesToSocket(OutputStream out, byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    private void newListener()
    {
        (new Thread(this)).start(); // All new threads start at the run() method where they will patiently wait for a new client connection
    }

    
    // TODO: refactor to also collect other info from the header
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