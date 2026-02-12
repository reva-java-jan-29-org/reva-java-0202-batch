package com.training.employeemgmt.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class DbConnectionFactory {
    private final String url;
    private final String user;
    private final String password;

    public DbConnectionFactory(String url, String user, String password) {
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    public Connection getConnection() throws SQLException {
        // For production: use a connection pool (HikariCP). This is fine for learning/interviews.
        return DriverManager.getConnection(url, user, password);
    }

    public static DbConnectionFactory fromEnv() {
        // Example env vars:
        // DB_URL="jdbc:mysql://localhost:3306/company_db?useSSL=false&serverTimezone=UTC"
        // DB_USER="root"
        // DB_PASSWORD="root123"
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/company_db?useSSL=false&serverTimezone=UTC");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "Root123");
        return new DbConnectionFactory(url, user, pass);
    }
}

