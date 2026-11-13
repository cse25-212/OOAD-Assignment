package com.bankingsystem.interfaces;

/**
 * Contract for payment instruments (cards) requiring PIN verification and payment processing.
 */
public interface Payable {
    boolean makePayment(double amount);
    boolean verifyPIN(int pin);
}
