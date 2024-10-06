// Holden Ernest - 10/6/2024

// parses and stores information on list metadata (permissions and whatnot)

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class ListMetaParser {
    
    private String owner;
    private String[] readAccess;
    private String[] writeAccess;

    ListMetaParser(byte[] metaFileBytes) {
        readAccess = new String[0];
        writeAccess = new String[0];
        parseBytes(metaFileBytes);
    }

    private void parseBytes(byte[] b) {
        String byteString = new String(b, StandardCharsets.UTF_8);
        Scanner sc = new Scanner(byteString);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            int sp = line.indexOf(": ");
            String oKey = line.substring(0, sp);
            String oValue = line.substring(sp+2);

            switch (oKey) {
                case "owner":
                    owner = oValue;
                    break;
                case "read":
                    toReadList(oValue);
                    break;
                case "write":
                    toWriteList(oValue);
                    break;
                default:
                    break;
            }
          }
          sc.close();
    }
    private void toReadList(String usersString) {
        readAccess = usersString.split(" ");
    }
    private void toWriteList(String usersString) {
        writeAccess = usersString.split(" ");
    }

    public Boolean hasReadAccess(String username) {
        if (readAccess.length == 0) return false;
        for (String user : readAccess) {
            if (user.equals(username)) return true;
        }
        return false;
    }
    public Boolean hasWriteAccess(String username) {
        if (writeAccess.length == 0) return false;
        for (String user : writeAccess) {
            if (user.equals(username)) return true;
        }
        return false;
    }
    public Boolean isOwner(String username) {
        return username.equals(owner);
    }
}
