package com.bankingsystem.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2-sha256$v1$";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        return hashValue(password);
    }

    public static String hashForMigration(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Legacy password must not be empty.");
        }
        return hashValue(password);
    }

    private static String hashValue(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String encodedHash) {
        if (password == null || !isEncodedHash(encodedHash)) {
            return false;
        }

        String[] parts = encodedHash.split("\\$", -1);
        int iterations = Integer.parseInt(parts[2]);
        byte[] salt = Base64.getDecoder().decode(parts[3]);
        byte[] expected = Base64.getDecoder().decode(parts[4]);
        if (iterations < 100_000 || iterations > 1_000_000
                || salt.length != SALT_BYTES || expected.length != HASH_BITS / 8) {
            return false;
        }
        return MessageDigest.isEqual(expected, derive(password, salt, iterations));
    }

    public static boolean isEncodedHash(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = value.split("\\$", -1);
        if (parts.length != 5) {
            return false;
        }
        if (!parts[2].matches("\\d{1,7}")) {
            return false;
        }
        try {
            Base64.getDecoder().decode(parts[3]);
            Base64.getDecoder().decode(parts[4]);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not hash the password.", exception);
        } finally {
            spec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }
}
