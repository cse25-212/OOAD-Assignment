package com.bankingsystem.model;

/**
 * Concrete Transaction representing a withdrawal from an Account.
 */
public class Withdrawal extends Transaction {
    private final Account account;

    public Withdrawal(String transactionID, double amount, Account account) {
        super(transactionID, amount, "WITHDRAWAL", account.getAccountNumber());
        this.account = account;
    }

    @Override
    public boolean execute() {
        if (isCancelled() || isExecuted()) {
            return false;
        }
        boolean success = account.withdraw(getAmount());
        if (success) {
            markExecuted();
        }
        return success;
    }
}
