package com.bankingsystem.dao;

import com.bankingsystem.security.CustomerDataCipher;
import com.bankingsystem.security.PasswordHasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Initializes the SQLite schema and inserts the 10 sample records on first run.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        initialize(CustomerDataCipher.fromEnvironment());
    }

    public static void initialize(CustomerDataCipher dataCipher) throws SQLException {
        if (dataCipher == null) {
            throw new IllegalArgumentException("Customer data cipher cannot be null.");
        }
        Path databaseFile = databaseFile();
        boolean databaseAlreadyExists;
        try {
            databaseAlreadyExists = databaseFile != null
                    && Files.exists(databaseFile)
                    && Files.size(databaseFile) > 0;
        } catch (IOException exception) {
            throw new SQLException("Could not inspect the SQLite database before migration.", exception);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (databaseAlreadyExists && hasCustomers(conn) && schemaVersion(conn) < 2) {
                backupDatabase(conn, databaseFile);
            }
            executeSqlScript(stmt, loadSql("database/schema.sql", DEFAULT_SCHEMA_SQL));
            conn.setAutoCommit(false);
            try {
                ensureCustomerSecurityColumns(conn);
                migrateCustomerData(conn, dataCipher);
                if (!hasCustomers(conn)) {
                    seedCustomers(conn, dataCipher);
                    executeSqlScript(stmt, loadSql("database/seed.sql", DEFAULT_SEED_SQL));
                }
                encryptRelatedData(conn, dataCipher);
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email_lookup "
                        + "ON customers(email_lookup)");
                stmt.execute("PRAGMA user_version = 2");
                conn.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    private static Path databaseFile() {
        String url = DatabaseConnection.getDatabaseUrl();
        String prefix = "jdbc:sqlite:";
        if (url == null || !url.startsWith(prefix)) {
            return null;
        }
        String fileName = url.substring(prefix.length());
        if (fileName.isBlank() || ":memory:".equals(fileName) || fileName.startsWith("file:")) {
            return null;
        }
        try {
            return Path.of(fileName).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not resolve the SQLite database path.", exception);
        }
    }

    private static int schemaVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean hasCustomers(Connection conn) throws SQLException {
        if (!tableExists(conn, "customers")) {
            return false;
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM customers LIMIT 1")) {
            return rs.next();
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName)
            throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void ensureCustomerSecurityColumns(Connection conn) throws SQLException {
        if (!columnExists(conn, "customers", "password_hash")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE customers ADD COLUMN password_hash TEXT");
            }
        }
        if (!columnExists(conn, "customers", "email_lookup")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE customers ADD COLUMN email_lookup TEXT");
            }
        }
    }

    private static void migrateCustomerData(Connection conn, CustomerDataCipher dataCipher) throws SQLException {
        boolean hasLegacyPassword = columnExists(conn, "customers", "password");
        String passwordColumn = hasLegacyPassword ? ", password" : "";
        List<CustomerMigrationRow> rows = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT customer_id, first_name, surname, email, phone_number, "
                     + "address, national_id, date_of_birth, company_name, company_address, registration_number, "
                     + "password_hash" + passwordColumn + " FROM customers")) {
            while (rs.next()) {
                rows.add(new CustomerMigrationRow(
                        rs.getString("customer_id"),
                        rs.getString("first_name"),
                        rs.getString("surname"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("address"),
                        rs.getString("national_id"),
                        rs.getString("date_of_birth"),
                        rs.getString("company_name"),
                        rs.getString("company_address"),
                        rs.getString("registration_number"),
                        rs.getString("password_hash"),
                        hasLegacyPassword ? rs.getString("password") : null
                ));
            }
        }

        String updateSql = "UPDATE customers SET first_name = ?, surname = ?, email = ?, phone_number = ?, "
                + "address = ?, national_id = ?, date_of_birth = ?, company_name = ?, company_address = ?, "
                + "registration_number = ?, password_hash = ?, email_lookup = ? WHERE customer_id = ?";
        try (PreparedStatement update = conn.prepareStatement(updateSql);
             PreparedStatement clearLegacyPassword = hasLegacyPassword
                     ? conn.prepareStatement("UPDATE customers SET password = '' WHERE customer_id = ?")
                     : null) {
            for (CustomerMigrationRow row : rows) {
                String existingHash = row.passwordHash();
                String rawPassword = row.legacyPassword();
                String passwordHash;
                if (PasswordHasher.isEncodedHash(existingHash)) {
                    passwordHash = existingHash;
                } else if (rawPassword != null && !rawPassword.isEmpty()) {
                    passwordHash = PasswordHasher.hashForMigration(rawPassword);
                } else if (existingHash != null && !existingHash.isEmpty()) {
                    passwordHash = PasswordHasher.hashForMigration(existingHash);
                } else {
                    throw new SQLException("Customer " + row.customerId()
                            + " has no usable password; database migration was stopped.");
                }

                String email = dataCipher.decryptIfNeeded(row.email());
                update.setString(1, dataCipher.encryptIfNeeded(row.firstName()));
                update.setString(2, dataCipher.encryptIfNeeded(row.surname()));
                update.setString(3, dataCipher.encryptIfNeeded(email));
                update.setString(4, dataCipher.encryptIfNeeded(row.phoneNumber()));
                update.setString(5, dataCipher.encryptIfNeeded(row.address()));
                update.setString(6, dataCipher.encryptIfNeeded(row.nationalId()));
                update.setString(7, dataCipher.encryptIfNeeded(row.dateOfBirth()));
                update.setString(8, dataCipher.encryptIfNeeded(row.companyName()));
                update.setString(9, dataCipher.encryptIfNeeded(row.companyAddress()));
                update.setString(10, dataCipher.encryptIfNeeded(row.registrationNumber()));
                update.setString(11, passwordHash);
                update.setString(12, dataCipher.emailLookup(email));
                update.setString(13, row.customerId());
                update.addBatch();

                if (clearLegacyPassword != null) {
                    clearLegacyPassword.setString(1, row.customerId());
                    clearLegacyPassword.addBatch();
                }
            }
            update.executeBatch();
            if (clearLegacyPassword != null) {
                clearLegacyPassword.executeBatch();
            }
        }
    }

    private static void encryptRelatedData(Connection conn, CustomerDataCipher dataCipher) throws SQLException {
        encryptColumns(conn, dataCipher, "employment_details", "customer_id",
                "company_name", "company_address");
        encryptColumns(conn, dataCipher, "accounts", "account_number",
                "employer_name", "employer_address");
    }

    private static void encryptColumns(Connection conn, CustomerDataCipher dataCipher, String table,
                                       String idColumn, String firstColumn, String secondColumn)
            throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + idColumn + ", " + firstColumn + ", "
                     + secondColumn + " FROM " + table)) {
            while (rs.next()) {
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
        }

        String sql = "UPDATE " + table + " SET " + firstColumn + " = ?, " + secondColumn
                + " = ? WHERE " + idColumn + " = ?";
        try (PreparedStatement update = conn.prepareStatement(sql)) {
            for (String[] row : rows) {
                update.setString(1, dataCipher.encryptIfNeeded(row[1]));
                update.setString(2, dataCipher.encryptIfNeeded(row[2]));
                update.setString(3, row[0]);
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private static void seedCustomers(Connection conn, CustomerDataCipher dataCipher) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    customer_id, customer_type, first_name, surname, email, phone_number, address,
                    national_id, date_of_birth, company_name, company_address, registration_number,
                    username, password_hash, email_lookup
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String[][] individuals = {
                {"CUST-001", "IndividualCustomer", "Tshepang", "Segobaetso", "tshepang@example.bw",
                        "+267 71000001", "Block 8, Gaborone", "123456789", "2002-05-14", null, null, null,
                        "tshepang.s", "pass1234"},
                {"CUST-002", "IndividualCustomer", "Kabo", "Motsumi", "kabo.motsumi@example.bw",
                        "+267 71000002", "Phakalane, Gaborone", "223456780", "1997-08-21", null, null, null,
                        "kabo.m", "pass1234"},
                {"CUST-003", "IndividualCustomer", "Neo", "Modise", "neo.modise@example.bw",
                        "+267 71000003", "Extension 9, Gaborone", "323456781", "1999-11-03", null, null, null,
                        "neo.m", "pass1234"},
                {"CUST-004", "IndividualCustomer", "Lorato", "Kgosi", "lorato.kgosi@example.bw",
                        "+267 71000004", "Area W, Francistown", "423456782", "1995-02-19", null, null, null,
                        "lorato.k", "pass1234"},
                {"CUST-005", "IndividualCustomer", "Thabo", "Ditlhokwa", "thabo.d@example.bw",
                        "+267 71000005", "Boseja, Maun", "523456783", "1992-07-11", null, null, null,
                        "thabo.d", "pass1234"},
                {"CUST-006", "IndividualCustomer", "Amantle", "Sefularo", "amantle.s@example.bw",
                        "+267 71000006", "Gaborone West Phase 1", "623456784", "2000-01-27", null, null, null,
                        "amantle.s", "pass1234"},
                {"CUST-007", "IndividualCustomer", "Bakang", "Mogapi", "bakang.m@example.bw",
                        "+267 71000007", "Tlokweng, Gaborone", "723456785", "1994-09-08", null, null, null,
                        "bakang.m", "pass1234"},
                {"CUST-008", "IndividualCustomer", "Boitumelo", "Rantao", "boitumelo.r@example.bw",
                        "+267 71000008", "Mogoditshane", "823456786", "1996-12-30", null, null, null,
                        "boitumelo.r", "pass1234"},
                {"CUST-009", "CorporateCustomer", "Mpho", "Kelebonye", "finance@kalahtech.bw",
                        "+267 3912001", "Fairgrounds, Gaborone", null, null, "Kalahari Tech (Pty) Ltd",
                        "Plot 50671, Fairgrounds, Gaborone", "BW-2026-8841", "kalahari.corp", "corp1234"},
                {"CUST-010", "CorporateCustomer", "Onalenna", "Tshiamo", "accounts@okavangosafaris.bw",
                        "+267 6861102", "Airport Road, Maun", null, null, "Okavango Eco Safaris",
                        "Plot 114, Maun", "BW-2025-4412", "okavango.corp", "corp1234"}
        };
        try (PreparedStatement insert = conn.prepareStatement(sql)) {
            for (String[] customer : individuals) {
                insert.setString(1, customer[0]);
                insert.setString(2, customer[1]);
                for (int i = 2; i < 12; i++) {
                    insert.setString(i + 1, dataCipher.encrypt(customer[i]));
                }
                insert.setString(13, customer[12]);
                insert.setString(14, PasswordHasher.hash(customer[13]));
                insert.setString(15, dataCipher.emailLookup(customer[4]));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void backupDatabase(Connection conn, Path databaseFile) throws SQLException {
        try {
            Files.createDirectories(databaseFile.getParent());
            Path backup = databaseFile.resolveSibling(databaseFile.getFileName() + ".before-security-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                    + "-" + UUID.randomUUID() + ".bak");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA wal_checkpoint(FULL)");
                stmt.execute("VACUUM INTO '" + backup.toString().replace("'", "''") + "'");
            }
            System.out.println("Legacy database backup created at: " + backup);
        } catch (IOException exception) {
            throw new SQLException("Could not create a backup before database migration.", exception);
        }
    }

    private record CustomerMigrationRow(
            String customerId,
            String firstName,
            String surname,
            String email,
            String phoneNumber,
            String address,
            String nationalId,
            String dateOfBirth,
            String companyName,
            String companyAddress,
            String registrationNumber,
            String passwordHash,
            String legacyPassword
    ) {
    }

    private static void executeSqlScript(Statement stmt, String sql) throws SQLException {
        String[] statements = sql.split(";");
        for (String raw : statements) {
            String trimmed = stripComments(raw).trim();
            if (!trimmed.isEmpty()) {
                stmt.execute(trimmed);
            }
        }
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String loadSql(String relativePath, String fallback) throws SQLException {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) {
            try {
                return Files.readString(p);
            } catch (IOException exception) {
                throw new SQLException("Could not read database script: " + p, exception);
            }
        }
        return fallback;
    }

    private static final String DEFAULT_SCHEMA_SQL = """
            PRAGMA foreign_keys = ON;
            CREATE TABLE IF NOT EXISTS branches (
                branch_code TEXT PRIMARY KEY,
                address TEXT NOT NULL,
                is_open INTEGER NOT NULL DEFAULT 1
            );
            CREATE TABLE IF NOT EXISTS customers (
                customer_id TEXT PRIMARY KEY,
                customer_type TEXT NOT NULL,
                first_name TEXT NOT NULL,
                surname TEXT NOT NULL,
                email TEXT NOT NULL,
                phone_number TEXT NOT NULL,
                address TEXT NOT NULL,
                national_id TEXT,
                date_of_birth TEXT,
                company_name TEXT,
                company_address TEXT,
                registration_number TEXT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                email_lookup TEXT NOT NULL UNIQUE
            );
            CREATE TABLE IF NOT EXISTS employment_details (
                customer_id TEXT PRIMARY KEY,
                company_name TEXT NOT NULL,
                company_address TEXT NOT NULL,
                is_employed INTEGER NOT NULL DEFAULT 1,
                verified INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS accounts (
                account_number TEXT PRIMARY KEY,
                account_type TEXT NOT NULL,
                balance REAL NOT NULL CHECK (balance >= 0),
                branch_code TEXT NOT NULL,
                customer_id TEXT NOT NULL,
                date_opened TEXT NOT NULL,
                interest_rate REAL NOT NULL DEFAULT 0.0,
                employer_name TEXT,
                employer_address TEXT,
                FOREIGN KEY (branch_code) REFERENCES branches(branch_code),
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id TEXT PRIMARY KEY,
                account_number TEXT NOT NULL,
                target_account_number TEXT,
                tx_type TEXT NOT NULL,
                amount REAL NOT NULL CHECK (amount > 0),
                reference TEXT,
                timestamp TEXT NOT NULL,
                executed INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS audit_logs (
                audit_id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_number TEXT NOT NULL,
                action TEXT NOT NULL,
                logged_at TEXT NOT NULL,
                FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
            );
            """;

    private static final String DEFAULT_SEED_SQL = """
            INSERT OR IGNORE INTO branches (branch_code, address, is_open) VALUES
            ('BR-001', 'Plot 54368, CBD, Gaborone', 1),
            ('BR-002', 'Blue Jacket Street, Francistown', 1),
            ('BR-003', 'Old Mall, Maun', 1);
            INSERT OR IGNORE INTO employment_details (customer_id, company_name, company_address, is_employed, verified) VALUES
            ('CUST-001', 'Botswana Innovation Hub', 'Plot 69184, Gaborone', 1, 1),
            ('CUST-002', 'Debswana Diamond Co.', 'Orapa House, Gaborone', 1, 1),
            ('CUST-003', 'Botswana Accountancy College', 'Fairgrounds, Gaborone', 1, 1),
            ('CUST-004', 'Tati Nickel Mining', 'Francistown Industrial', 1, 1),
            ('CUST-005', 'Water Utilities Corp', 'Maun Regional Office', 1, 1);
            INSERT OR IGNORE INTO accounts (account_number, account_type, balance, branch_code, customer_id, date_opened, interest_rate, employer_name, employer_address) VALUES
            ('SAV-1001', 'SavingsAccount', 2500.00, 'BR-001', 'CUST-001', '2026-10-12', 0.0005, NULL, NULL),
            ('INV-2001', 'InvestmentAccount', 1500.00, 'BR-001', 'CUST-001', '2026-10-12', 0.0500, NULL, NULL),
            ('CHQ-3001', 'ChequeAccount', 12700.00, 'BR-001', 'CUST-001', '2026-10-12', 0.0000, 'Botswana Innovation Hub', 'Plot 69184, Gaborone'),
            ('SAV-1002', 'SavingsAccount', 4800.00, 'BR-001', 'CUST-002', '2026-10-14', 0.0005, NULL, NULL),
            ('CHQ-3002', 'ChequeAccount', 9200.00, 'BR-001', 'CUST-002', '2026-10-14', 0.0000, 'Debswana Diamond Co.', 'Orapa House, Gaborone'),
            ('INV-2003', 'InvestmentAccount', 3200.00, 'BR-001', 'CUST-003', '2026-10-15', 0.0500, NULL, NULL),
            ('CHQ-3004', 'ChequeAccount', 6450.00, 'BR-002', 'CUST-004', '2026-10-18', 0.0000, 'Tati Nickel Mining', 'Francistown Industrial'),
            ('SAV-1005', 'SavingsAccount', 1850.00, 'BR-003', 'CUST-005', '2026-10-20', 0.0005, NULL, NULL),
            ('INV-2006', 'InvestmentAccount', 5000.00, 'BR-001', 'CUST-006', '2026-10-22', 0.0500, NULL, NULL),
            ('INV-2009', 'InvestmentAccount', 45000.00, 'BR-001', 'CUST-009', '2026-10-25', 0.0500, NULL, NULL);
            INSERT OR IGNORE INTO transactions (transaction_id, account_number, target_account_number, tx_type, amount, reference, timestamp, executed) VALUES
            ('TX-0001', 'SAV-1001', NULL, 'DEPOSIT', 2500.00, 'Opening Deposit', '2026-10-12T09:00:00', 1),
            ('TX-0002', 'INV-2001', NULL, 'DEPOSIT', 1500.00, 'Opening Investment >= BWP500', '2026-10-12T09:10:00', 1),
            ('TX-0003', 'CHQ-3001', NULL, 'SALARY', 12500.00, 'October Salary Credit', '2026-10-25T10:00:00', 1),
            ('TX-0004', 'SAV-1002', NULL, 'DEPOSIT', 4800.00, 'Savings Deposit', '2026-10-14T11:15:00', 1),
            ('TX-0005', 'CHQ-3002', NULL, 'WITHDRAWAL', 800.00, 'ATM Cash Withdrawal', '2026-10-26T14:20:00', 1),
            ('TX-0006', 'INV-2003', NULL, 'INTEREST', 152.38, 'Monthly 5% Investment Yield', '2026-10-31T23:59:00', 1),
            ('TX-0007', 'CHQ-3004', 'SAV-1005', 'TRANSFER', 350.00, 'Family Support Transfer', '2026-11-01T08:45:00', 1),
            ('TX-0008', 'SAV-1005', NULL, 'INTEREST', 0.92, 'Monthly 0.05% Savings Yield', '2026-10-31T23:59:00', 1),
            ('TX-0009', 'INV-2006', NULL, 'DEPOSIT', 5000.00, 'Capital Top-up', '2026-10-22T15:30:00', 1),
            ('TX-0010', 'INV-2009', NULL, 'DEPOSIT', 45000.00, 'Corporate Treasury Placement', '2026-10-25T16:05:00', 1);
            """;
}
