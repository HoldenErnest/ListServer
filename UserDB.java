// Holden Ernest - 10/4/2024

// A User DB, stored by hashing and salting passwords
// With this you can create, edit, and retrieve users

import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class UserDB {

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256; // The hash is always 256 bits(32 Bytes)

    private static ArrayList<User> allUsers = new ArrayList<User>(); 

    public static void initDB() {

    }

    public static void createUser(String user, char[] pass) { // INTERFACE for the user to interact with (from ClassFileServer CLI you 'user add', which will call this method)
        if (hasUser(user)) return;
        
        User newUser = createNewUserHash(user, pass); // TODO: maybe log this new user creation
        try {
            saveUser(newUser, false);
            allUsers.add(newUser);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
    public static Boolean hasUser(String user, char[] pass) { // hasUser(user) and password for that user is correct
        User u = tryGetUser(user);

        if (u == null) return false;

        if (u.testPassword(pass)) return true;

        return false;
    }
    public static Boolean hasUser(String user) { // does the db contain a user named this
        if (tryGetUser(user) != null) return true;
        return false;
    }
    private static User tryGetUser(String user) { // try to find and return a user from a username. Returns null if nothing found ----------// TODO: FIX THIS, this controlls how fast the users get searched (its linear rn)
        // attempt to find it in the list in ram
        for (User u : allUsers) {
            if (u.getUsername().equals(user)) return u;
        }
        // attempt to read it from hard drive
        try {
            return tryDownloadUser(user);
        } catch (Exception e) {}

        // you didnt find it :(
        return null;
        
    }

    private static User createNewUserHash(String user, char[] pass) { // create the hash based off a password. Then save the resulting hash and salt
        byte[] salt = UserDB.getNextSalt();
        byte[] hashPass = hash(pass, salt);
        return new User(user, salt, hashPass);
        //put the salt and has into the db at key user (users are unique)

        //keys.put("salt", new String(salt, StandardCharsets.UTF_8));
        //keys.put("hash", new String(hashPass, StandardCharsets.UTF_8));
        //allUsers.put(user, keys);
    }

    private static byte[] getNextSalt() { // magic using secureRandom (BUT salt is always 16 bytes)
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }
    private static byte[] hash(char[] password, byte[] salt) { // salt the password, then return the hash of that
        String hashAlg = "PBKDF2WithHmacSHA1";
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE); // clear password
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(hashAlg);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }
    public static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) { // returns whether the given password works with the specified 
        byte[] pwdHash = hash(password, salt);
        Arrays.fill(password, Character.MIN_VALUE);
        if (pwdHash.length != expectedHash.length) return false;
        for (int i = 0; i < pwdHash.length; i++) {
          if (pwdHash[i] != expectedHash[i]) return false;
        }
        return true;
    }


    private static void saveUser(User u, Boolean overwrite) throws IOException { // Read a certain file from the docroot
        byte[] userByteString = u.toBytes();
        File f = new File(ClassFileServer.getUsersPath() + u.getUsername());
        int length = (int)(f.length());
        if (length == 0 || overwrite) { // if it doesnt exist or you can overwrite it, start writing
            System.out.println("[FILE] Writing to file: " + ClassFileServer.getUsersPath() + u.getUsername());
            FileOutputStream outputStream = new FileOutputStream(f);
            outputStream.write(userByteString);
            outputStream.close();
        } else {
            throw new IOException("[FILE] Cannot overwrite: " + ClassFileServer.getUsersPath() + u.getUsername());
        }
    }
    private static User tryDownloadUser(String username) throws IOException { // Read a certain file from the users files
        File f = new File(ClassFileServer.getUsersPath() + username);
        int length = (int)(f.length());
        if (length == 0) {
            throw new IOException("File length is zero: " + username);
        } else {
            System.out.println("[FILE] Downloading user: " + username);
            FileInputStream fin = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fin);

            byte[] bytecodes = new byte[length];
            in.readFully(bytecodes);
            User u = new User(username, bytecodes);
            allUsers.add(u);
            return u;
        }
    }
}