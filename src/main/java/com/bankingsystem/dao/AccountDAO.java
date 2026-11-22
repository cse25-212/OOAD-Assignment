package com.bankingsystem.dao;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.ChequeAccount;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.InvestmentAccount;
import com.bankingsystem.model.SavingsAccount;
import com.bankingsystem.security.CustomerDataCipher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object for polymorphic Account CRUD operations.
 */
public class AccountDAO {
    private final CustomerDataCipher dataCipher;

    public AccountDAO() {
        this(CustomerDataCipher.fromEnvironment());
    }

    public AccountDAO(CustomerDataCipher dataCipher) {
        if (dataCipher == null) {
            throw new IllegalArgumentException("Customer data cipher cannot be null.");
        }
        this.dataCipher = dataCipher;
    }

    public void create(Account account) throws SQLException {
        String sql = """
                INSERT INTO accounts (
                    account_number, account_type, balance, branch_code, customer_id,
                    date_opened, interest_rate, employer_name, employer_address
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, account.getAccountNumber());
                    ps.setString(2, account.getAccountType());
                    ps.setDouble(3, account.getBalance());
                    ps.setString(4, account.getBranchCode());
                    ps.setString(5, account.getCustomerId());
                    ps.setString(6, account.getDateOpened().toString());

                    if (account instanceof SavingsAccount) {
                        ps.setDouble(7, SavingsAccount.INTEREST_RATE);
                        ps.setString(8, null);
                        ps.setString(9, null);
                    } else if (account instanceof InvestmentAccount) {
                        ps.setDouble(7, InvestmentAccount.INTEREST_RATE);
                        ps.setString(8, null);
                        ps.setString(9, null);
                    } else if (account instanceof ChequeAccount cheque) {
                        ps.setDouble(7, 0.0);
                        ps.setString(8, dataCipher.encrypt(cheque.getEmployerName()));
                        ps.setString(9, dataCipher.encrypt(cheque.getEmployerAddress()));
                    } else {
                        throw new SQLException("Unsupported account type: " + account.getAccountType());
                    }
                    ps.executeUpdate();
                }
                if (account instanceof ChequeAccount cheque) {
                    saveEmploymentDetails(conn, account.getCustomerId(), cheque.getEmploymentDetails());
                }
                conn.commit();
            } catch (SQLException exception) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Account> findByCustomerId(String customerId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE customer_id = ? ORDER BY account_number";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAccount(rs));
                }
            }
        }
        return list;
    }

    public List<Account> findAll() throws SQLException {
        String sql = "SELECT * FROM accounts ORDER BY account_number";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapAccount(rs));
            }
        }
        return list;
    }

    public String nextAccountNumber(String accountTypePrefix) throws SQLException {
        if (accountTypePrefix == null || !accountTypePrefix.matches("SAV|INV|CHQ")) {
            throw new IllegalArgumentException("Unsupported account type prefix.");
        }
        String sql = """
                SELECT COALESCE(MAX(CAST(SUBSTR(account_number, INSTR(account_number, '-') + 1) AS INTEGER)), 0) + 1
                FROM accounts
                WHERE account_number LIKE ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountTypePrefix + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                int sequence = rs.next() ? rs.getInt(1) : 1;
                return accountTypePrefix + "-" + String.format("%04d", sequence);
            }
        }
    }

    public boolean updateBalance(String accountNumber, double newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, accountNumber);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String accountNumber) throws SQLException {
        String sql = "DELETE FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            return ps.executeUpdate() > 0;
        }
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        String number = rs.getString("account_number");
        String type = rs.getString("account_type");
        double balance = rs.getDouble("balance");
        String branchCode = rs.getString("branch_code");
        String customerId = rs.getString("customer_id");
        LocalDate dateOpened = LocalDate.parse(rs.getString("date_opened"));

        return switch (type) {
            case "SavingsAccount" -> SavingsAccount.fromPersistence(number, balance, branchCode, customerId, dateOpened);
            case "InvestmentAccount" -> InvestmentAccount.fromPersistence(number, balance, branchCode, customerId, dateOpened);
            case "ChequeAccount" -> {
                EmploymentDetails emp = new EmploymentDetails(
                        dataCipher.decrypt(rs.getString("employer_name")),
                        dataCipher.decrypt(rs.getString("employer_address")),
                        true
                );
                emp.verifyDetails();
                yield ChequeAccount.fromPersistence(number, balance, branchCode, customerId, emp, dateOpened);
            }
            default -> throw new SQLException("Unknown account_type in database: " + type);
        };
    }

    private void saveEmploymentDetails(Connection conn, String customerId,
                                      EmploymentDetails details) throws SQLException {
        String sql = """
                INSERT INTO employment_details (customer_id, company_name, company_address, is_employed, verified)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(customer_id) DO UPDATE SET
                    company_name = excluded.company_name,
                    company_address = excluded.company_address,
                    is_employed = excluded.is_employed,
                    verified = excluded.verified
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, dataCipher.encrypt(details.getCompanyName()));
            ps.setString(3, dataCipher.encrypt(details.getCompanyAddress()));
            ps.setInt(4, details.isEmployed() ? 1 : 0);
            ps.setInt(5, details.verifyDetails() ? 1 : 0);
            ps.executeUpdate();
        }
    }
}
