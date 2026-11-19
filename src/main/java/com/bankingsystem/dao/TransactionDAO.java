package com.bankingsystem.dao;

import com.bankingsystem.model.Account;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object for persisted Transaction and Audit Log records.
 */
public class TransactionDAO {

    public record TransactionRecord(
            String transactionId,
            String accountNumber,
            String targetAccountNumber,
            String txType,
            double amount,
            String reference,
            String timestamp,
            boolean executed
    ) {
    }

    public void create(String transactionId, String accountNumber, String targetAccountNumber,
                       String txType, double amount, String reference, boolean executed) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = transactionStatement(conn, transactionId, accountNumber,
                     targetAccountNumber, txType, amount, reference, executed)) {
            ps.executeUpdate();
        }
    }

    public void persistBalanceAndTransaction(Account account, String transactionId, String txType,
                                             double amount, String reference) throws SQLException {
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setDouble(1, account.getBalance());
                    update.setString(2, account.getAccountNumber());
                    requireUpdatedAccount(update.executeUpdate(), account.getAccountNumber());
                }
                try (PreparedStatement insert = transactionStatement(conn, transactionId,
                        account.getAccountNumber(), null, txType, amount, reference, true)) {
                    insert.executeUpdate();
                }
                conn.commit();
            } catch (SQLException exception) {
                rollback(conn, exception);
                throw exception;
            }
        }
    }

    public void persistTransfer(Account source, Account target, String transactionId,
                                double amount, String reference) throws SQLException {
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setDouble(1, source.getBalance());
                    update.setString(2, source.getAccountNumber());
                    requireUpdatedAccount(update.executeUpdate(), source.getAccountNumber());

                    update.setDouble(1, target.getBalance());
                    update.setString(2, target.getAccountNumber());
                    requireUpdatedAccount(update.executeUpdate(), target.getAccountNumber());
                }
                try (PreparedStatement insert = transactionStatement(conn, transactionId,
                        source.getAccountNumber(), target.getAccountNumber(), "TRANSFER",
                        amount, reference, true)) {
                    insert.executeUpdate();
                }
                conn.commit();
            } catch (SQLException exception) {
                rollback(conn, exception);
                throw exception;
            }
        }
    }

    public Optional<TransactionRecord> findById(String transactionId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRecord(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<TransactionRecord> findByAccountNumber(String accountNumber) throws SQLException {
        String sql = """
                SELECT * FROM transactions
                WHERE account_number = ? OR target_account_number = ?
                ORDER BY timestamp DESC
                """;
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        }
        return list;
    }

    public List<TransactionRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
        List<TransactionRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRecord(rs));
            }
        }
        return list;
    }

    public boolean updateReference(String transactionId, String newReference) throws SQLException {
        String sql = "UPDATE transactions SET reference = ? WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newReference);
            ps.setString(2, transactionId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String transactionId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            return ps.executeUpdate() > 0;
        }
    }

    public void logAudit(String accountNumber, String action) throws SQLException {
        String sql = "INSERT INTO audit_logs (account_number, action, logged_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, action);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    private TransactionRecord mapRecord(ResultSet rs) throws SQLException {
        return new TransactionRecord(
                rs.getString("transaction_id"),
                rs.getString("account_number"),
                rs.getString("target_account_number"),
                rs.getString("tx_type"),
                rs.getDouble("amount"),
                rs.getString("reference"),
                rs.getString("timestamp"),
                rs.getInt("executed") == 1
        );
    }

    private PreparedStatement transactionStatement(Connection conn, String transactionId, String accountNumber,
                                                   String targetAccountNumber, String txType, double amount,
                                                   String reference, boolean executed) throws SQLException {
        String sql = """
                INSERT INTO transactions (
                    transaction_id, account_number, target_account_number, tx_type,
                    amount, reference, timestamp, executed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, transactionId);
        ps.setString(2, accountNumber);
        ps.setString(3, targetAccountNumber);
        ps.setString(4, txType);
        ps.setDouble(5, amount);
        ps.setString(6, reference);
        ps.setString(7, LocalDateTime.now().toString());
        ps.setInt(8, executed ? 1 : 0);
        return ps;
    }

    private void requireUpdatedAccount(int rows, String accountNumber) throws SQLException {
        if (rows != 1) {
            throw new SQLException("Account not found while saving: " + accountNumber);
        }
    }

    private void rollback(Connection conn, SQLException cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
