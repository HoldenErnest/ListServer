// Holden Ernest - 10/4/2024

// an object to parse a header buffer and store the information relevant to it.

import java.io.*;
import java.net.*;
import javax.net.*;
import java.util.Date;
import java.util.List;

public class HeaderParser {
    private String filePath = ""; // Do I need this?
    private String line = "";
    private String user = "";
    private String pass = "";
    private String mode = ""; // 'login', 'perms', 'get', 'save'
    private String host = ""; // IS THIS THE IP OF THE SENDER?
    private String listID = "";
    
    public void parseHeader(BufferedReader in) throws Exception { // returns if the operation succeeded without issue
        parse(in);
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
                        listID = oValue.replaceAll("[\\?%*:|\"<>]", "-"); // dissallow everything but / (for incase the file is from another user)
                        break;
                    case "Host":
                        host = oValue;
                        break;
                    case "Mode":
                        mode = oValue;
                        break;
                }
            } catch (Exception e) {
                System.out.println("[Parse] Unexpected line: " + line);
            }
            line = in.readLine();
        } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        System.out.println(infoString());
        if (!UserDB.hasUser(getUsername(), getPassword().toCharArray())) throw new Exception("Invalid User");
    }

    public String getListPath() throws Exception {
        if (user.length() == 0 || listID.length() == 0) {
            throw new Exception("Cannot get a valid path from this header");
        }

        int delim = listID.indexOf("/");
        if (delim > 1 && listID.length() - delim >= 1)  { // the username(before /) is real and the listName(after /) is real
            int delim2 = listID.indexOf("/",delim+1);
            if (delim2 > 0) {
                throw new Exception("ListID is invalid");
            }
            return listID + ".csv";
        } else if (delim > 0) {
            throw new Exception("ListID is invalid");
        }

        return user + File.separator + listID + ".csv"; // users/jimmy/aList.csv
    }
    public String getMode() {
        return mode;
    }
    public String getUsername() {
        return user;
    }
    public String getPassword() {
        return pass;
    }

    public String infoString() {
       return String.format("{User: \"%s\", Pass: \"%s\", Mode: \"%s\", List: \"%s\", Host: \"%s\"}", user, pass, mode, listID, host);
    }
} 