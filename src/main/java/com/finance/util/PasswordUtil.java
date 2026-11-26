package com.finance.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class for BCrypt password hashing and verification.
 */
public final class PasswordUtil {

    private static final int BCRYPT_COST = 10;

    private PasswordUtil() {
    }

    /**
     * Hashes a plaintext password using BCrypt with a random salt.
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Checks whether a stored password value is already a BCrypt hash.
     */
    public static boolean isHashed(String storedPassword) {
        if (storedPassword == null || storedPassword.length() != 60) {
            return false;
        }
        return storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$");
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash (or legacy plaintext password).
     */
    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            try {
                return BCrypt.checkpw(plainPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        // Legacy plaintext fallback so existing accounts can be upgraded transparently on login.
        return MessageDigest.isEqual(
                plainPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
