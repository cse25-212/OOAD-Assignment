package com.bankingsystem.model;

import com.bankingsystem.interfaces.Depositable;
import com.bankingsystem.interfaces.Transferable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class representing a customer bank Account.
 * Demonstrates abstraction, encapsulation, method overloading, and polymorphism.
 */
public abstract class Account implements Depositable, Transferable {
    private final String accountNumber;
    private double balance;
    private final String branchCode;
    private final String customerId;
    private final LocalDate dateOpened;
    private final TransactionHistory transactionHistory;
    private final List<Card> cards;

    protected Account(String accountNumber, double initialBalance, String branchCode, String customerId) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be blank.");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("An account cannot exist without a holding customer.");
        }
        if (branchCode == null || branchCode.isBlank()) {
            throw new IllegalArgumentException("Branch code must not be blank.");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.branchCode = branchCode;
        this.customerId = customerId;
        this.dateOpened = LocalDate.now();
        this.transactionHistory = new TransactionHistory();
        this.cards = new ArrayList<>();
        this.transactionHistory.logAudit(
                String.format("Account %s opened at branch %s for customer %s with initial balance BWP %.2f",
                        accountNumber, branchCode, customerId, initialBalance)
        );
    }

    @Override
    public void deposit(double amount) {
        deposit(amount, "Standard Deposit");
    }

    @Override
    public void deposit(double amount, String reference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        this.balance += amount;
        String ref = (reference == null || reference.isBlank()) ? "Deposit" : reference;
        this.transactionHistory.logAudit(
                String.format("Credited BWP %.2f to %s (%s). New balance: BWP %.2f",
                        amount, accountNumber, ref, balance)
        );
    }

    @Override
    public boolean transfer(double amount, Account targetAccount) {
        if (targetAccount == null || targetAccount == this || amount <= 0) {
            return false;
        }
        if (!debit(amount)) {
            return false;
        }
        targetAccount.deposit(amount, "Transfer from " + this.accountNumber);
        this.transactionHistory.logAudit(
                String.format("Transferred BWP %.2f from %s to %s",
                        amount, this.accountNumber, targetAccount.getAccountNumber())
        );
        return true;
    }

    public boolean debit(double amount) {
        if (amount <= 0 || amount > balance) {
            return false;
        }
        this.balance -= amount;
        return true;
    }

    protected void setBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance;
    }

    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null.");
        }
        if (!cards.contains(card)) {
            cards.add(card);
            transactionHistory.logAudit("Card issued and linked: " + card.getCardNumber());
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    public TransactionHistory getTransactionHistory() {
        return transactionHistory;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public abstract boolean withdraw(double amount);

    public abstract String getAccountType();
}
