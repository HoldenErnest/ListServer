// Holden Ernest - 10/5/2024
// Contains all information on a given user, parsed from a userfile

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class User {
    private String username;
    private byte[] hash = new byte[32];
    private byte[] salt = new byte[16];
    // potentially more metadata in the future

    User(String username, byte[] byteFile) { // parse in a user from a bytefile
        this.username = username;
        fromBytes(byteFile);
    }
    User(String username, byte[] salt, byte[] hash) {
        this.salt = salt;
        this.hash = hash;
        this.username = username;
    }

    public byte[] toBytes() throws IOException { // convert this into bytes that can be written striaght to a file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        //outputStream.write(username.getBytes(StandardCharsets.UTF_8));
        //outputStream.write((byte)'\n');

        // More metadata can be added to the output stream here

        outputStream.write( salt );//16B
        outputStream.write( hash );//32B
        //System.out.println("saltlen: " + salt.length + ". hashlen: " + hash.length);
        byte out[] = outputStream.toByteArray( ); // TODO MAKE SURE THIS LOOKS NICE IN THE FILE / CAN IT BE PARSED
        return out;
    }
    private void fromBytes(byte[] userfilebytes) {
        try {
            System.arraycopy(userfilebytes, 0, salt, 0, 16);
            System.arraycopy(userfilebytes, 16, hash, 0, 32);
            //System.out.println("successfully copied " + username);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
    public String getUsername() {
        return username;
    }

    // I dont want to make any public methods that return the salt or the hash, however I can make proxxy methods which compare their hashes to UserDB information.
    public Boolean testPassword(char[] pass) { // returns true if password works
        return UserDB.isExpectedPassword(pass, salt, hash);
    }

    
}
