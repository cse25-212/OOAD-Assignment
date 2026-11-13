package com.bankingsystem.model;

/**
 * Concrete Transaction representing a deposit into a target Account.
 */
public class Deposit extends Transaction {
    private final Account account;
    private final String reference;

    public Deposit(String transactionID, double amount, Account account, String reference) {
        super(transactionID, amount, "DEPOSIT", account.getAccountNumber());
        this.account = account;
        this.reference = reference;
    }

    @Override
    public boolean execute() {
        if (isCancelled() || isExecuted()) {
            return false;
        }
        account.deposit(getAmount(), reference != null ? reference : "Direct Deposit");
        markExecuted();
        return true;
    }
}
