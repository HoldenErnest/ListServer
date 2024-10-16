// Holden Ernest - 10/6/2024

// parses and stores information on list metadata (permissions and whatnot)

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class ListMetaParser {
    
    private String owner;
    private String[] readAccess;
    private String[] writeAccess;
    private int version = -1;

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
                case "version":
                    version = Integer.parseInt(oValue);
                    break;
                default:
                    break;
            }
          }
          sc.close();
    }

    public byte[] getBytes() {
        String metafile = "owner: " + owner + "\nread: "; // + "\nwrite: " + "\nversion: -1"
        for (String r : readAccess) {
            metafile += r + " ";
        }
        metafile += "\nwrite: ";
        for (String w : writeAccess) {
            metafile += w + " ";
        }
        metafile += "\nversion: " + version;

        return metafile.getBytes(StandardCharsets.UTF_8);
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
    public Boolean removePermissions(String user) { // returns whether a change was made.
        removeRead(user);
        return true;
    }
    private void removeWrite(String user) {
        if (hasWriteAccess(user)) {
            String[] newWriteArr = new String[writeAccess.length - 1];
            int offset = 0;
            for (int i = 0; i < writeAccess.length; i++) {
                if (!writeAccess[i].equals(user)) {
                    newWriteArr[i-offset] = writeAccess[i];
                    offset++;
                }
            }
            writeAccess = newWriteArr;
        }
    }
    private void removeRead(String user) {
        if (hasReadAccess(user)) {
            String[] newReadArr = new String[readAccess.length - 1];
            int offset = 0;
            for (int i = 0; i < readAccess.length; i++) {
                if (!readAccess[i].equals(user)) {
                    newReadArr[i-offset] = readAccess[i];
                    offset++;
                }
            }
            readAccess = newReadArr;
        }
        removeWrite(user);
    }
    public Boolean tryAllowWrite(String user) { // returns if the list changed
        if (hasWriteAccess(user)) return false;
        String[] newWriteArr = new String[writeAccess.length + 1];
        for (int i = 0; i < writeAccess.length; i++) {
            newWriteArr[i] = writeAccess[i];
        }
        newWriteArr[newWriteArr.length-1] = user;
        writeAccess = newWriteArr;
        tryAllowRead(user, true);
        return true;
    }
    public Boolean tryAllowRead(String user, Boolean hasWrite) { // returns if the list changed
        if (!hasReadAccess(user)) {
            String[] newReadArr = new String[readAccess.length + 1];
            for (int i = 0; i < readAccess.length; i++) {
                newReadArr[i] = readAccess[i];
            }
            newReadArr[newReadArr.length-1] = user;
            readAccess = newReadArr;
        }
        if (!hasWrite) removeWrite(user); // if you set a guy to read you might want to remove write.
        return true;
    }
    public Boolean isOwner(String username) {
        return username.equals(owner);
    }
    public void setVersion(int i) {
        version = i;
    }
    public Boolean olderThanSameAs(int version) { // returns if the saved list is strictly Older Than the version that was passed
        return this.version <= version;
    }
    public Boolean newerThan(int version) { // returns if the saved list is strictly Newer Than the version that was passed
        return this.version > version;
    }
    public Boolean sameVersion(int version) {
        return this.version == version;
    }
    public int getVersion() {
        return version;
    }
}
