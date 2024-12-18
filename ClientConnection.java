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
import java.nio.charset.StandardCharsets;

import javax.net.*;
import java.util.Date;

public abstract class ClientConnection implements Runnable {

    private ServerSocket server = null;
    private BufferedWriter log;

    private String senderIP;

    protected ClientConnection(ServerSocket ss) {// this is only called once to accept the first connection
        server = ss;
        
        try {
            log = new BufferedWriter(new FileWriter("log", true));
        } catch (Exception e) {
            System.out.println("Problem Setting up log file");
        }

        newListener();
        System.out.println("[LIVE] Awaiting Connection..");
        
    }

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
            senderIP = srcAddr + ":" + srcPort;
            log("Connection attempt at " + senderIP);
        } catch (IOException e) {
            System.out.println("Server died: " + e.getMessage());
            e.printStackTrace();
            return; // end this thread
        }

        // create a new thread to wait for if there are any more connections
        newListener();

        try {
            OutputStream rawOut = socket.getOutputStream(); // This output is used to directly send bytes (so use for sending file data)
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(rawOut))); // this output is used to send text only

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ConnectionParser header = new ConnectionParser();

                header.parseHeader(in); // this can throw an exception which will send a 400 page

                log("Connection success at " + senderIP + " INFO: " + header.infoString());
                // The metadata for the packet was collected so now do stuff with it.

                Response sender = getInfo(header);

                // The information to send back has been gathered, so send it.

                try {
                    writeHeaderToSocket(out, sender);
                    writeTextToSocket(out, sender);

                    //byte[] data = getBytes(header.getPath());  // write back file if it was requested
                    //writeBytesToSocket(rawOut,data);

                } catch (Exception ie) { // cant send info.
                    ie.printStackTrace();
                    System.out.println("PROBLEM::::: CANNOT WRITE TO SOCKET, socket not closed");
                    return;
                }

            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("ERROR: " + e);
                // write out error response
                writeErrorPage(out, e.getMessage());
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

    private void writeErrorPage(PrintWriter out, String m) {
        out.println("HTTP/1.0 400 " + m + "\r\n");
        out.println("Received Bad Info: " + m + "\r\n\r\n");
        out.flush();
    }
    private void writeHeaderToSocket(PrintWriter out, Response r) {
        out.print("HTTP/1.0 "+r.getStatus()+" OK\r\n");
        out.print("Content-Length: " + r.getData().length() + "\r\n");
        out.print("Content-Type: text/html\r\n");
        out.print("Version: "+r.getVersion()+"\r\n\r\n");
        out.flush();
    }
    private void writeTextToSocket(PrintWriter out, Response sender) {
        out.print(sender.getData());
        out.flush();
    }
    private void writeBytesToSocket(OutputStream out, byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    // One of the most important methods, from the header info return whatever you need to send as a string
    private Response getInfo(ConnectionParser h) throws Exception {
        Response builder = new Response();

        switch (h.getMode()) {
            case "login": // test if login Username and Password is in DB
                getLoginModeInfo(h, builder);
                break;
            case "get": // load and send a list specified by the user
                getLoadModeInfo(h, builder);
                break;
            case "lists": // give a list of all Available Lists to the user.
                getAllLists(h, builder);
                break;
            case "save": // attempt to save a list to this server, on fail, it will be reported.
                getSaveModeInfo(h, builder);
                break;
            default:
                throw new Exception("Invalid Received Mode");
        }
        return builder;
    }
    private void getLoginModeInfo(ConnectionParser h, Response res) throws Exception {
        res.setData("200 HasUser: true\n");
    }
    private void getLoadModeInfo(ConnectionParser h, Response res) throws Exception {
        byte[] listBytes = Server.getList(h, res,true);
        res.setData(new String(listBytes, StandardCharsets.UTF_8)); // begins with 200 or 300
    }
    private void getSaveModeInfo(ConnectionParser h, Response res) throws Exception {
        Server.saveList(h, res);
        //data could be set inside the chain.
    }
    private void getAllLists(ConnectionParser h, Response res) throws Exception{
        Server.getAllLists(h,res);
    }

    // END MODES ---------------------------------------------------------------------------

    private void newListener() {
        (new Thread(this)).start(); // All new threads start at the run() method where they will patiently wait for a new client connection
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