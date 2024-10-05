// Holden Ernest - 10/4/2024

// an object to parse a header buffer and store the information relevant to it.

import java.io.*;
import java.net.*;
import javax.net.*;
import java.util.Date;

public class HeaderParser {
    private String filePath = ""; // Do I need this?
    private String line = "";
    private String user = "";
    private String pass = "";
    private String host = ""; // IS THIS THE IP OF THE SENDER?
    private String listID = "";
    
    public Boolean parseHeader(BufferedReader in) { // returns if the operation succeeded without issue
        try {
            parse(in);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void parse(BufferedReader in) throws Exception { // Technically I want to make it differ from HTTP so that it cant be mistakenly accessed 
        
        
        
        // Read in the first line to make sure its a good connection
        line = in.readLine();
        if (!line.startsWith("LUPU /")) {
            throw new Exception("Incorrect Method");
        }
        line = in.readLine();

        do {
            try {
                int sp = line.indexOf(": ");
                String oKey = line.substring(0, sp);
                String oValue = line.substring(sp+2);

                //System.out.println("key " + oKey + " val " + oValue);
                switch (oKey) {
                    case "User":
                        user = oValue;
                        break;
                    case "Pass":
                        pass = oValue;
                        break;
                    case "List":
                        listID = oValue;
                        break;
                    case "Host":
                        host = oValue;
                        break;
                }
            } catch (Exception e) {
                System.out.println("[Parse] Unexpected line: " + line);
            }
            line = in.readLine();
        } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        System.out.println(infoString());
    }

    public String getPath() {
        return filePath; // this path might be gotten from user/listID.csv
    }

    public String infoString() {
       return String.format("{User: \"%s\", Pass: \"%s\", List: \"%s\", Host: \"%s\"}", user, pass, listID, host);
    }
} 