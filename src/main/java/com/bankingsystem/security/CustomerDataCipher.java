package com.bankingsystem.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class CustomerDataCipher {
    private static final String PREFIX = "enc:v1:";
    private static final String ENVIRONMENT_VARIABLE = "BANKING_DATA_KEY";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec emailLookupKey;

    public CustomerDataCipher(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("BANKING_DATA_KEY must contain a base64-encoded 32-byte key.");
        }

        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("BANKING_DATA_KEY must be valid base64.", exception);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalArgumentException("BANKING_DATA_KEY must decode to exactly 32 bytes.");
        }

        encryptionKey = new SecretKeySpec(derive(key, "pii-encryption"), "AES");
        emailLookupKey = new SecretKeySpec(derive(key, "email-lookup"), "HmacSHA256");
        Arrays.fill(key, (byte) 0);
    }

    public static CustomerDataCipher fromEnvironment() {
        String key = System.getenv(ENVIRONMENT_VARIABLE);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Set BANKING_DATA_KEY to a base64-encoded 32-byte secret before starting the banking system."
            );
        }
        return new CustomerDataCipher(key);
    }

    public String encrypt(String value) {
        if (value == null) {
            return null;
        }

        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt customer data.", exception);
        }
    }

    public String decrypt(String value) {
        if (value == null) {
            return null;
        }
        if (!isEncrypted(value)) {
            throw new IllegalArgumentException("Customer data is not encrypted.");
        }

        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Stored customer data has an invalid encrypted format.", exception);
        }
        if (payload.length <= IV_BYTES) {
            throw new IllegalStateException("Stored customer data is incomplete.");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
        byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not decrypt customer data; verify BANKING_DATA_KEY matches the database key.", exception
            );
        }
    }

    public String encryptIfNeeded(String value) {
        return value == null || isEncrypted(value) ? value : encrypt(value);
    }

    public String decryptIfNeeded(String value) {
        return value == null || !isEncrypted(value) ? value : decrypt(value);
    }

    public String emailLookup(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(emailLookupKey);
            byte[] digest = mac.doFinal(email.trim().toLowerCase(java.util.Locale.ROOT)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not protect the email lookup value.", exception);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static byte[] derive(byte[] key, String purpose) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not derive a customer-data key.", exception);
        }
    }
}
