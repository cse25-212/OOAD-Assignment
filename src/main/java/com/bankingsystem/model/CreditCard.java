package com.bankingsystem.model;

/**
 * CreditCard linked to a customer Account with a revolving credit limit and tracked debt.
 */
public class CreditCard extends Card {
    private final double creditLimit;
    private double currentDebt;

    public CreditCard(String cardNumber, String expiryDate, int csv, int pin,
                      Account linkedAccount, double creditLimit) {
        super(cardNumber, expiryDate, csv, pin, linkedAccount);
        if (creditLimit <= 0) {
            throw new IllegalArgumentException("Credit limit must be positive.");
        }
        this.creditLimit = creditLimit;
        this.currentDebt = 0.0;
    }

    @Override
    public boolean makePayment(double amount) {
        if (amount <= 0 || amount > currentDebt) {
            return false;
        }
        if (getLinkedAccount().withdraw(amount)) {
            currentDebt -= amount;
            getLinkedAccount().getTransactionHistory().logAudit(
                    String.format("CreditCard %s repayment of BWP %.2f. Remaining debt: BWP %.2f",
                            getCardNumber(), amount, currentDebt)
            );
            return true;
        }
        return false;
    }

    public boolean charge(double amount) {
        if (amount <= 0 || (currentDebt + amount) > creditLimit) {
            return false;
        }
        currentDebt += amount;
        getLinkedAccount().getTransactionHistory().logAudit(
                String.format("CreditCard %s charged BWP %.2f. Current debt: BWP %.2f",
                        getCardNumber(), amount, currentDebt)
        );
        return true;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public double getCurrentDebt() {
        return currentDebt;
    }
}
