package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConfig {

    private static volatile DBConfig instance;

    private String url;
    private String username;
    private String password;

    private DBConfig() {
        // Connection parameters can be overridden (in order of precedence):
        //   1. JVM system property:  -Ddb.password=secret
        //   2. environment variable: DB_PASSWORD=secret
        //   3. built-in default below
        this.url = resolve("db.url", "DB_URL",
                "jdbc:mysql://localhost:3306/bank_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        this.username = resolve("db.username", "DB_USER", "root");
        this.password = resolve("db.password", "DB_PASSWORD", "paswd");

        try {
            // Load and register MySQL JDBC Driver explicitly
            Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver");
            java.sql.Driver driver = (java.sql.Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
        } catch (Exception e) {
            System.err.println("Warning: MySQL JDBC Driver initialization: " + e.getMessage());
        }
    }

    private static String resolve(String systemProperty, String envVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVariable);
        }
        if (value == null || value.isBlank()) {
            value = defaultValue;
        }
        return value;
    }

    public static DBConfig getInstance() {
        if (instance == null) {
            synchronized (DBConfig.class) {
                if (instance == null) {
                    instance = new DBConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Establishes and returns a new JDBC Database Connection.
     * Automatically creates database and schema tables if bank_db does not exist.
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown database"))) {
                System.out.println("Database 'bank_db' not found. Executing automatic database & schema setup...");
                if (createDatabaseAndSchema()) {
                    return DriverManager.getConnection(url, username, password);
                }
            }
            throw e;
        }
    }

    public boolean createDatabaseAndSchema() {
        String dbName = extractDatabaseName(url);
        String baseUrl = extractBaseUrl(url);

        if (dbName == null || baseUrl == null) {
            System.err.println("Could not parse database name or base URL from: " + url);
            return false;
        }

        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("Database '" + dbName + "' created successfully.");

            try (Connection dbConn = DriverManager.getConnection(url, username, password);
                 Statement dbStmt = dbConn.createStatement()) {

                initializeTables(dbStmt);
                System.out.println("Database schema tables and seed data initialized successfully.");
            }
            return true;
        } catch (SQLException ex) {
            System.err.println("Failed to auto-create database/schema: " + ex.getMessage());
            return false;
        }
    }

    private void initializeTables(Statement stmt) throws SQLException {
        // 1. Users Table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL,
                CONSTRAINT chk_user_role CHECK (role IN ('Admin', 'Customer'))
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // 2. Customers Table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS customers (
                customer_id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) NOT NULL,
                phone VARCHAR(20) NOT NULL,
                address TEXT,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // 3. Accounts Table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS accounts (
                account_number BIGINT PRIMARY KEY,
                customer_id INT NOT NULL,
                balance DOUBLE NOT NULL DEFAULT 0.0,
                account_type VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'Active',
                open_date DATE NOT NULL,
                CONSTRAINT chk_account_type CHECK (account_type IN ('Savings', 'Current')),
                CONSTRAINT chk_account_status CHECK (status IN ('Active', 'Closed')),
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // 4. Transactions Table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                account_number BIGINT NOT NULL,
                type VARCHAR(20) NOT NULL,
                amount DOUBLE NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                description VARCHAR(255),
                CONSTRAINT chk_trans_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
                FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // Seed initial admin and customer data if users table is empty
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("""
                    INSERT INTO users (user_id, username, password, role) VALUES
                    (1, 'admin', 'admin123', 'Admin'),
                    (2, 'san', 'user123', 'Customer'),
                    (3, 'jane_smith', 'user123', 'Customer')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO customers (customer_id, user_id, name, email, phone, address) VALUES
                    (101, 2, 'San', 'san@example.com', '+1-555-0192', '123 Main Street, New York, NY'),
                    (102, 3, 'Jane Smith', 'jane.smith@example.com', '+1-555-0148', '456 Oak Avenue, Los Angeles, CA')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO accounts (account_number, customer_id, balance, account_type, status, open_date) VALUES
                    (1000000001, 101, 5000.00, 'Savings', 'Active', '2025-01-15'),
                    (1000000002, 102, 2500.50, 'Current', 'Active', '2025-02-01')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO transactions (account_number, type, amount, description) VALUES
                    (1000000001, 'DEPOSIT', 5000.00, 'Initial Account Opening Deposit'),
                    (1000000002, 'DEPOSIT', 2500.50, 'Initial Account Opening Deposit')
                    """);
            }
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        try {
            int cleanPos = jdbcUrl.indexOf('?');
            String cleanUrl = (cleanPos != -1) ? jdbcUrl.substring(0, cleanPos) : jdbcUrl;
            int lastSlash = cleanUrl.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < cleanUrl.length() - 1) {
                return cleanUrl.substring(lastSlash + 1);
            }
        } catch (Exception ignored) {}
        return "bank_db";
    }

    private String extractBaseUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        try {
            String dbName = extractDatabaseName(jdbcUrl);
            if (dbName != null) {
                return jdbcUrl.replace("/" + dbName, "/");
            }
        } catch (Exception ignored) {}
        return "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing Connection: " + e.getMessage());
            }
        }
    }

    public static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing Statement: " + e.getMessage());
            }
        }
        closeConnection(conn);
    }
}
