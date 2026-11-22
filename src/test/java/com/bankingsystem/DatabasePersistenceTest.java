package com.bankingsystem;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.DatabaseConnection;
import com.bankingsystem.dao.DatabaseInitializer;
import com.bankingsystem.dao.TransactionDAO;
import com.bankingsystem.interfaces.InterestPayable;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.Authentication;
import com.bankingsystem.model.ChequeAccount;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.IndividualCustomer;
import com.bankingsystem.security.CustomerDataCipher;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceTest {
    private static final CustomerDataCipher DATA_CIPHER =
            new CustomerDataCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

    @Test
    void initializesSampleDataAndPersistsAccountTransactions() throws Exception {
        String originalUrl = DatabaseConnection.getDatabaseUrl();
        Path databaseFile = Files.createTempFile("banking-system-test-", ".db");
        Files.deleteIfExists(databaseFile);
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());

        try {
            DatabaseInitializer.initialize(DATA_CIPHER);

            CustomerDAO customerDAO = new CustomerDAO(DATA_CIPHER);
            assertEquals(11, customerDAO.nextCustomerSequence());
            assertTrue(customerDAO.authenticate("tshepang.s", "pass1234").isPresent());

            String registrationPassword = "registration-secret";
            IndividualCustomer registeredCustomer = new IndividualCustomer(
                    "CUST-011", "Test", "Person", "test.person@example.com", "+267 71000011",
                    "Test Address", new Authentication("test.person", registrationPassword),
                    "ID-TEST-011", LocalDate.of(2001, 2, 3));
            customerDAO.create(registeredCustomer);
            assertTrue(customerDAO.authenticate("test.person", registrationPassword).isPresent());
            try (var conn = DatabaseConnection.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(
                         "SELECT first_name, email, password_hash FROM customers WHERE customer_id = 'CUST-011'")) {
                assertTrue(rs.next());
                assertTrue(CustomerDataCipher.isEncrypted(rs.getString("first_name")));
                assertTrue(CustomerDataCipher.isEncrypted(rs.getString("email")));
                assertFalse(rs.getString("password_hash").contains(registrationPassword));
            }

            AccountDAO accountDAO = new AccountDAO(DATA_CIPHER);
            assertEquals("SAV-1006", accountDAO.nextAccountNumber("SAV"));
            assertEquals("INV-2010", accountDAO.nextAccountNumber("INV"));
            assertEquals("CHQ-3005", accountDAO.nextAccountNumber("CHQ"));
            Account account = accountDAO.findByAccountNumber("SAV-1001").orElseThrow();
            assertEquals(LocalDate.of(2026, 10, 12), account.getDateOpened());
            double previousBalance = account.getBalance();
            account.deposit(25.00);

            TransactionDAO transactionDAO = new TransactionDAO();
            transactionDAO.persistBalanceAndTransaction(
                    account, "TX-TEST-001", "DEPOSIT", 25.00, "Persistence test");

            Account reloaded = accountDAO.findByAccountNumber("SAV-1001").orElseThrow();
            assertEquals(previousBalance + 25.00, reloaded.getBalance(), 0.001);
            assertTrue(transactionDAO.findByAccountNumber("SAV-1001").stream()
                    .anyMatch(record -> "TX-TEST-001".equals(record.transactionId())));

            Account source = accountDAO.findByAccountNumber("INV-2001").orElseThrow();
            Account target = accountDAO.findByAccountNumber("SAV-1001").orElseThrow();
            double sourceBalance = source.getBalance();
            double targetBalance = target.getBalance();
            assertTrue(source.transfer(10.00, target));
            transactionDAO.persistTransfer(source, target, "TX-TEST-002", 10.00, "Transfer test");

            assertEquals(sourceBalance - 10.00,
                    accountDAO.findByAccountNumber("INV-2001").orElseThrow().getBalance(), 0.001);
            assertEquals(targetBalance + 10.00,
                    accountDAO.findByAccountNumber("SAV-1001").orElseThrow().getBalance(), 0.001);
            assertTrue(transactionDAO.findByAccountNumber("SAV-1001").stream()
                    .anyMatch(record -> "TX-TEST-002".equals(record.transactionId())));

            ChequeAccount cheque = new ChequeAccount(
                    "CHQ-TEST-001", 100.00, "BR-001", "CUST-001",
                    new EmploymentDetails("Test Employer", "Gaborone", true));
            accountDAO.create(cheque);
            assertEquals("Test Employer",
                    customerDAO.findEmploymentDetails("CUST-001").orElseThrow().getCompanyName());
        } finally {
            DatabaseConnection.setDatabaseUrl(originalUrl);
            Files.deleteIfExists(databaseFile);
        }
    }

    @Test
    void migratesLegacyPlaintextCustomersAndKeepsCredentialsUsable() throws Exception {
        String originalUrl = DatabaseConnection.getDatabaseUrl();
        Path databaseFile = Files.createTempFile("banking-system-legacy-", ".db");
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());

        try {
            try (var conn = DatabaseConnection.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE customers (
                            customer_id TEXT PRIMARY KEY,
                            customer_type TEXT NOT NULL,
                            first_name TEXT NOT NULL,
                            surname TEXT NOT NULL,
                            email TEXT NOT NULL UNIQUE,
                            phone_number TEXT NOT NULL,
                            address TEXT NOT NULL,
                            national_id TEXT,
                            date_of_birth TEXT,
                            company_name TEXT,
                            company_address TEXT,
                            registration_number TEXT,
                            username TEXT NOT NULL UNIQUE,
                            password TEXT NOT NULL
                        )
                        """);
                stmt.execute("""
                        INSERT INTO customers VALUES (
                            'CUST-001', 'IndividualCustomer', 'Test', 'Customer', 'test@example.com',
                            '12345678', 'Test Street', 'ID-123', '2000-01-02', NULL, NULL, NULL,
                            'test.user', 'shortpw'
                        )
                        """);
            }

            DatabaseInitializer.initialize(DATA_CIPHER);
            CustomerDAO customerDAO = new CustomerDAO(DATA_CIPHER);
            assertTrue(customerDAO.authenticate("test.user", "shortpw").isPresent());
            assertEquals("Test", customerDAO.findById("CUST-001").orElseThrow().getFirstName());

            try (var conn = DatabaseConnection.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(
                         "SELECT first_name, email, password_hash, password FROM customers WHERE customer_id = 'CUST-001'")) {
                assertTrue(rs.next());
                assertTrue(CustomerDataCipher.isEncrypted(rs.getString("first_name")));
                assertTrue(CustomerDataCipher.isEncrypted(rs.getString("email")));
                assertTrue(com.bankingsystem.security.PasswordHasher.isEncodedHash(rs.getString("password_hash")));
                assertEquals("", rs.getString("password"));
            }

            try (var files = Files.list(databaseFile.getParent())) {
                assertTrue(files.anyMatch(path -> path.getFileName().toString()
                        .startsWith(databaseFile.getFileName() + ".before-security-")));
            }
        } finally {
            DatabaseConnection.setDatabaseUrl(originalUrl);
            try (var files = Files.list(databaseFile.getParent())) {
                files.filter(path -> path.getFileName().toString().startsWith(databaseFile.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        });
            }
        }
    }

    @Test
    void rollsBackEveryBalanceAndTransactionWhenInterestBatchFails() throws Exception {
        String originalUrl = DatabaseConnection.getDatabaseUrl();
        Path databaseFile = Files.createTempFile("banking-system-interest-", ".db");
        Files.deleteIfExists(databaseFile);
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());

        try {
            DatabaseInitializer.initialize(DATA_CIPHER);
            AccountDAO accountDAO = new AccountDAO(DATA_CIPHER);
            Account savings = accountDAO.findByAccountNumber("SAV-1001").orElseThrow();
            Account investment = accountDAO.findByAccountNumber("INV-2001").orElseThrow();
            double savingsBalance = savings.getBalance();
            double investmentBalance = investment.getBalance();
            double savingsInterest = ((InterestPayable) savings).applyInterest();
            double investmentInterest = ((InterestPayable) investment).applyInterest();
            TransactionDAO transactionDAO = new TransactionDAO();

            assertThrows(java.sql.SQLException.class, () -> transactionDAO.persistInterestBatch(List.of(
                    new TransactionDAO.InterestEntry(savings, "TX-INTEREST-DUPLICATE",
                            savingsInterest, "Interest test"),
                    new TransactionDAO.InterestEntry(investment, "TX-INTEREST-DUPLICATE",
                            investmentInterest, "Interest test")
            )));

            assertEquals(savingsBalance,
                    accountDAO.findByAccountNumber("SAV-1001").orElseThrow().getBalance(), 0.001);
            assertEquals(investmentBalance,
                    accountDAO.findByAccountNumber("INV-2001").orElseThrow().getBalance(), 0.001);
            assertTrue(transactionDAO.findById("TX-INTEREST-DUPLICATE").isEmpty());
        } finally {
            DatabaseConnection.setDatabaseUrl(originalUrl);
            Files.deleteIfExists(databaseFile);
        }
    }
}
