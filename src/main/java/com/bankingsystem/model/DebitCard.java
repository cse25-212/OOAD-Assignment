package com.bankingsystem.model;

/**
 * DebitCard linked to a customer Account with a daily withdrawal limit.
 */
public class DebitCard extends Card {
    private final double dailyWithdrawalLimit;
    private double withdrawnToday;

    public DebitCard(String cardNumber, String expiryDate, int csv, int pin,
                     Account linkedAccount, double dailyWithdrawalLimit) {
        super(cardNumber, expiryDate, csv, pin, linkedAccount);
        if (dailyWithdrawalLimit <= 0) {
            throw new IllegalArgumentException("Daily withdrawal limit must be positive.");
        }
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.withdrawnToday = 0.0;
    }

    @Override
    public boolean makePayment(double amount) {
        if (amount <= 0) {
            return false;
        }
        return getLinkedAccount().withdraw(amount);
    }

    public boolean dispenseFromATM(double amount) {
        if (amount <= 0 || (withdrawnToday + amount) > dailyWithdrawalLimit) {
            return false;
        }
        if (getLinkedAccount().withdraw(amount)) {
            withdrawnToday += amount;
            return true;
        }
        return false;
    }

    public double getDailyWithdrawalLimit() {
        return dailyWithdrawalLimit;
    }

    public double getWithdrawnToday() {
        return withdrawnToday;
    }
}
