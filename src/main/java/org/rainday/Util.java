package org.rainday;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by admin on 2019/10/28 17:04:24.
 */
public class Util {
    public static String calcPass(String pass, long minute) {
        String foo;
        { // first hash
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                return null; // null will not be the same of any string
            }
            sha256.update(pass.getBytes());
            foo = Base64.getEncoder().encodeToString(sha256.digest());
        }
        foo += minute;
        { // second hash
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                return null; // null will not be the same of any string
            }
            sha256.update(foo.getBytes());
            return Base64.getEncoder().encodeToString(sha256.digest());
        }
    }
}
