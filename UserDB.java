// Holden Ernest - 10/4/2024

// A User DB, stored by hashing and salting passwords
// With this you can create, edit, and retrieve users

import org.json.*;
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

    private static JSONObject allUsers; // TODO: find a way to not have to use json
    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    public static void initDB() {
        // load json file, pass into the constructor as a jsonstring
        allUsers = new JSONObject();
    }

    public static void createUser(String user, char[] pass) { // INTERFACE for the user to interact with
        if (!hasUser(user))
            saveNewUserHash(user, pass);

        System.out.println(allUsers.toString());
    }
    public static Boolean hasUser(String user, char[] pass) { // hasUser(user) and password for that user is correct
        if (!hasUser(user)) return false;
        // To be implimented
        return false;
    }
    public static Boolean hasUser(String user) { // does the db contain a user named this
        // To be implimented
        return false;
    }

    private static void saveNewUserHash(String user, char[] pass) { // create the hash based off a password. Then save the resulting hash and salt
        byte[] salt = UserDB.getNextSalt();
        byte[] hashPass = hash(pass, salt);

        //put the salt and has into the db at key user (users are unique)
        JSONObject keys = new JSONObject();
        keys.put("salt", new String(salt, StandardCharsets.UTF_8));
        keys.put("hash", new String(hashPass, StandardCharsets.UTF_8));
        allUsers.put(user, keys);
    }

    private static byte[] getNextSalt() { // magic using secureRandom
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
}