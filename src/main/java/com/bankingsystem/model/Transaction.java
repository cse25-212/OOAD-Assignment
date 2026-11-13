package com.bankingsystem.model;

import java.time.LocalDateTime;

/**
 * Abstract base class representing a financial transaction bound to an Account.
 */
public abstract class Transaction {
    private final String transactionID;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String type;
    private final String accountNumber;
    private boolean executed;
    private boolean cancelled;

    protected Transaction(String transactionID, double amount, String type, String accountNumber) {
        if (transactionID == null || transactionID.isBlank()) {
            throw new IllegalArgumentException("Transaction ID must not be blank.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }
        this.transactionID = transactionID;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.accountNumber = accountNumber;
        this.executed = false;
        this.cancelled = false;
    }

    public abstract boolean execute();

    public void cancel() {
        if (!executed) {
            this.cancelled = true;
        }
    }

    protected void markExecuted() {
        this.executed = true;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] Account=%s Amount=BWP %.2f Time=%s Executed=%s",
                type, transactionID, accountNumber, amount, timestamp, executed);
    }
}
