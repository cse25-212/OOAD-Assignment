package com.bankingsystem.model;

/**
 * Concrete Transaction representing a transfer between two Accounts.
 */
public class Transfer extends Transaction {
    private final Account sourceAccount;
    private final Account destinationAccount;
    private final String targetAccount;

    public Transfer(String transactionID, double amount, Account sourceAccount, Account destinationAccount) {
        super(transactionID, amount, "TRANSFER", sourceAccount.getAccountNumber());
        if (destinationAccount == null) {
            throw new IllegalArgumentException("Target account cannot be null.");
        }
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.targetAccount = destinationAccount.getAccountNumber();
    }

    @Override
    public boolean execute() {
        if (isCancelled() || isExecuted()) {
            return false;
        }
        boolean success = sourceAccount.transfer(getAmount(), destinationAccount);
        if (success) {
            markExecuted();
        }
        return success;
    }

    public String getTargetAccount() {
        return targetAccount;
    }
}
