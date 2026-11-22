package com.bankingsystem;

import com.bankingsystem.security.CustomerDataCipher;
import com.bankingsystem.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordSecurityTest {
    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void passwordsAreSaltedAndVerifiedWithoutStoringTheOriginalValue() {
        String password = "correct horse battery";
        String firstHash = PasswordHasher.hash(password);
        String secondHash = PasswordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertFalse(firstHash.contains(password));
        assertTrue(PasswordHasher.verify(password, firstHash));
        assertFalse(PasswordHasher.verify("wrong password", firstHash));
    }

    @Test
    void customerDataUsesAuthenticatedEncryptionAndStableEmailLookup() {
        CustomerDataCipher cipher = new CustomerDataCipher(TEST_KEY);
        String encrypted = cipher.encrypt("customer@example.com");

        assertNotEquals("customer@example.com", encrypted);
        assertEquals("customer@example.com", cipher.decrypt(encrypted));
        assertTrue(CustomerDataCipher.isEncrypted(cipher.encrypt("enc:v1:plain-text")));
        assertEquals(cipher.emailLookup("Customer@example.com"), cipher.emailLookup("customer@example.com"));
        assertThrows(IllegalStateException.class, () ->
                new CustomerDataCipher("AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=")
                        .decrypt(encrypted));
    }
}
