package com.bankingsystem.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages JDBC connections to the SQLite database for Part B Section 7.
 */
public final class DatabaseConnection {
    private static String databaseUrl = "jdbc:sqlite:banking_system.db";

    private DatabaseConnection() {
    }

    public static void setDatabaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL must not be blank.");
        }
        databaseUrl = url;
    }

    public static String getDatabaseUrl() {
        return databaseUrl;
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(databaseUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
