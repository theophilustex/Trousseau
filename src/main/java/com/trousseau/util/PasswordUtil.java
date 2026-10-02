package com.trousseau.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plaintext, String hashed) {
        return BCrypt.checkpw(plaintext, hashed);
    }
}
