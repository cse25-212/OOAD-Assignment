package com.bankingsystem.model;

import com.bankingsystem.interfaces.Payable;

/**
 * Base class for cards linked to a customer account.
 */
public abstract class Card implements Payable {
    private final String cardNumber;
    private final String expiryDate;
    private final int csv;
    private final int pin;
    private final Account linkedAccount;

    protected Card(String cardNumber, String expiryDate, int csv, int pin, Account linkedAccount) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number must not be blank.");
        }
        if (linkedAccount == null) {
            throw new IllegalArgumentException("Card must be linked to an account.");
        }
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.csv = csv;
        this.pin = pin;
        this.linkedAccount = linkedAccount;
    }

    @Override
    public boolean verifyPIN(int enteredPIN) {
        return enteredPIN == pin;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getCsv() {
        return csv;
    }

    public Account getLinkedAccount() {
        return linkedAccount;
    }
}
