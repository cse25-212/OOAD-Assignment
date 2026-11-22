package com.bankingsystem.model;

import com.bankingsystem.interfaces.Auditable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records all transactions and audit trail entries for an Account.
 */
public class TransactionHistory implements Auditable {
    private int totalTransactions;
    private final List<Transaction> transactions;
    private final List<String> auditLogs;

    public TransactionHistory() {
        this.totalTransactions = 0;
        this.transactions = new ArrayList<>();
        this.auditLogs = new ArrayList<>();
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }
        transactions.add(transaction);
        totalTransactions = transactions.size();
        logAudit("Transaction recorded: " + transaction.getType() + " [" + transaction.getTransactionID()
                + "] BWP " + String.format("%.2f", transaction.getAmount()));
    }

    @Override
    public void logAudit(String action) {
        if (action == null || action.isBlank()) {
            return;
        }
        auditLogs.add(LocalDateTime.now() + " | " + action);
    }

    @Override
    public List<String> getAuditHistory() {
        return Collections.unmodifiableList(auditLogs);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public Snapshot snapshot() {
        return new Snapshot(new ArrayList<>(transactions), new ArrayList<>(auditLogs));
    }

    public void restore(Snapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Transaction history snapshot cannot be null.");
        }
        transactions.clear();
        transactions.addAll(snapshot.transactions);
        auditLogs.clear();
        auditLogs.addAll(snapshot.auditLogs);
        totalTransactions = transactions.size();
    }

    public static final class Snapshot {
        private final List<Transaction> transactions;
        private final List<String> auditLogs;

        private Snapshot(List<Transaction> transactions, List<String> auditLogs) {
            this.transactions = transactions;
            this.auditLogs = auditLogs;
        }
    }
}
