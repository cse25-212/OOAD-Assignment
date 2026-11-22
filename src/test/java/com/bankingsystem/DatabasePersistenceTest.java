package com.bankingsystem;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.DatabaseConnection;
import com.bankingsystem.dao.DatabaseInitializer;
import com.bankingsystem.dao.TransactionDAO;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.ChequeAccount;
import com.bankingsystem.model.EmploymentDetails;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceTest {

    @Test
    void initializesSampleDataAndPersistsAccountTransactions() throws Exception {
        String originalUrl = DatabaseConnection.getDatabaseUrl();
        Path databaseFile = Files.createTempFile("banking-system-test-", ".db");
        Files.deleteIfExists(databaseFile);
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());

        try {
            DatabaseInitializer.initialize();

            CustomerDAO customerDAO = new CustomerDAO();
            assertEquals(11, customerDAO.nextCustomerSequence());
            assertTrue(customerDAO.authenticate("tshepang.s", "pass1234").isPresent());

            AccountDAO accountDAO = new AccountDAO();
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
}
