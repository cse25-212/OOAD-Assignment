package com.bankingsystem.model;

import com.bankingsystem.interfaces.InterestPayable;
import java.time.LocalDate;

/**
 * SavingsAccount earns 0.05% monthly interest and does not allow any withdrawals.
 */
public class SavingsAccount extends Account implements InterestPayable {
    public static final double INTEREST_RATE = 0.0005;

    public SavingsAccount(String accountNumber, double initialBalance, String branchCode, String customerId) {
        super(accountNumber, initialBalance, branchCode, customerId);
    }

    public static SavingsAccount fromPersistence(String accountNumber, double balance,
                                                  String branchCode, String customerId,
                                                  LocalDate dateOpened) {
        return new SavingsAccount(accountNumber, balance, branchCode, customerId, dateOpened);
    }

    private SavingsAccount(String accountNumber, double initialBalance, String branchCode,
                           String customerId, LocalDate dateOpened) {
        super(accountNumber, initialBalance, branchCode, customerId, dateOpened);
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
                    String.format("Savings monthly interest (0.05%%) applied: +BWP %.4f. New balance: BWP %.2f",
                            interest, getBalance())
            );
        }
        return interest;
    }

    @Override
    public boolean withdraw(double amount) {
        getTransactionHistory().logAudit(
                String.format("Withdrawal of BWP %.2f rejected: SavingsAccount does not permit withdrawals.", amount)
        );
        return false;
    }

    @Override
    public String getAccountType() {
        return "SavingsAccount";
    }
}
