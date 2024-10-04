// Holden Ernest - 10/4/2024

// an object to parse a header buffer and store the information relevant to it.

import java.io.*;
import java.net.*;
import javax.net.*;
import java.util.Date;

public class HeaderParser {
    private String filePath;
    
    public Boolean parseHeader(BufferedReader in) { // returns if the operation succeeded without issue
        try {
            parse(in);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void parse(BufferedReader in) throws Exception { // Technically I want to make it differ from HTTP so that it cant be mistakenly accessed 
        
        String line;
        
        line = in.readLine();
        if (!line.startsWith("LUPU /")) {
            throw new Exception("Incorrect Method");
        }
        do {
            System.out.println("[Parse] line: " + line);
            line = in.readLine();
        } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
    }

    public String getPath() {
        return filePath;
    }
} 