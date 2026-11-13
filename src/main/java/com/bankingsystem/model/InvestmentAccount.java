package com.bankingsystem.model;

import com.bankingsystem.interfaces.InterestPayable;
import com.bankingsystem.interfaces.Withdrawable;

/**
 * InvestmentAccount earns 5% monthly interest, allows withdrawals,
 * and requires a minimum opening deposit of BWP 500.00.
 */
public class InvestmentAccount extends Account implements InterestPayable, Withdrawable {
    public static final double INTEREST_RATE = 0.05;
    public static final double MIN_OPENING_DEPOSIT = 500.00;

    public InvestmentAccount(String accountNumber, double initialDeposit, String branchCode, String customerId) {
        super(accountNumber, validateOpeningDeposit(initialDeposit), branchCode, customerId);
    }

    private static double validateOpeningDeposit(double initialDeposit) {
        if (initialDeposit < MIN_OPENING_DEPOSIT) {
            throw new IllegalArgumentException(
                    String.format("InvestmentAccount requires a minimum opening deposit of BWP %.2f.", MIN_OPENING_DEPOSIT)
            );
        }
        return initialDeposit;
    }

    @Override
    public double calculateMonthlyInterest() {
        return getBalance() * INTEREST_RATE;
    }

    @Override
    public double applyInterest() {
        double interest = calculateMonthlyInterest();
        if (interest > 0) {
            setBalance(getBalance() + interest);
            getTransactionHistory().logAudit(
                    String.format("Investment monthly interest (5.00%%) applied: +BWP %.2f. New balance: BWP %.2f",
                            interest, getBalance())
            );
        }
        return interest;
    }

    @Override
    public boolean withdraw(double amount) {
        if (debit(amount)) {
            getTransactionHistory().logAudit(
                    String.format("Withdrew BWP %.2f from InvestmentAccount %s. Remaining balance: BWP %.2f",
                            amount, getAccountNumber(), getBalance())
            );
            return true;
        }
        getTransactionHistory().logAudit(
                String.format("Failed withdrawal attempt of BWP %.2f on InvestmentAccount %s.", amount, getAccountNumber())
        );
        return false;
    }

    @Override
    public String getAccountType() {
        return "InvestmentAccount";
    }
}
