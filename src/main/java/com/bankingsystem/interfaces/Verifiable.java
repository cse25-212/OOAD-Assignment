package com.bankingsystem.interfaces;

/**
 * Contract for verifiable customer or employment records.
 */
public interface Verifiable {
    boolean verifyDetails();
    boolean getVerificationStatus();
}
