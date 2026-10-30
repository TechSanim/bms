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

    private volatile boolean schemaChecked = false;

    private DBConfig() {

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
            Connection conn = DriverManager.getConnection(url, username, password);
            ensureSchemaUpToDate(conn);
            return conn;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown database"))) {
                System.out.println("Database 'bank_db' not found. Executing automatic database & schema setup...");
                if (createDatabaseAndSchema()) {
                    Connection conn = DriverManager.getConnection(url, username, password);
                    ensureSchemaUpToDate(conn);
                    return conn;
                }
            }
            throw e;
        }
    }

    /**
     * Applies idempotent schema migrations once per process so that databases
     * created by older versions are upgraded without losing data.
     */
    private void ensureSchemaUpToDate(Connection conn) {
        if (schemaChecked) return;
        synchronized (DBConfig.class) {
            if (schemaChecked) return;
            try {
                migrateSchema(conn);
                schemaChecked = true;
            } catch (SQLException e) {
                System.err.println("Schema migration warning: " + e.getMessage());
            }
        }
    }

    private void migrateSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            initializeTables(stmt);

            addColumnIfMissing(stmt, "users", "is_online",
                    "ALTER TABLE users ADD COLUMN is_online TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(stmt, "users", "last_login",
                    "ALTER TABLE users ADD COLUMN last_login DATETIME NULL");

            stmt.executeUpdate("ALTER TABLE users MODIFY COLUMN username VARCHAR(50) NULL");
            stmt.executeUpdate("ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL");

            ensureRoleConstraint(stmt);

            stmt.executeUpdate("""
                INSERT INTO users (username, password, role)
                SELECT 'headadmin', 'headadmin123', 'HeadAdmin'
                WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'headadmin')
                """);
        }
    }

    private void addColumnIfMissing(Statement stmt, String table, String column, String ddl) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (var ps = stmt.getConnection().prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate(ddl);
                }
            }
        }
    }

    private void ensureRoleConstraint(Statement stmt) {
        try {
            stmt.executeUpdate("ALTER TABLE users DROP CHECK chk_user_role");
        } catch (SQLException ignored) {
        }
        try {
            stmt.executeUpdate("ALTER TABLE users ADD CONSTRAINT chk_user_role "
                    + "CHECK (role IN ('Admin', 'Customer', 'HeadAdmin'))");
        } catch (SQLException ignored) {
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
                username VARCHAR(50) NULL UNIQUE,
                password VARCHAR(255) NULL,
                role VARCHAR(20) NOT NULL,
                is_online TINYINT(1) NOT NULL DEFAULT 0,
                last_login DATETIME NULL,
                CONSTRAINT chk_user_role CHECK (role IN ('Admin', 'Customer', 'HeadAdmin'))
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

        // 5. Admin Activity Log Table (login / logout audit trail)
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS admin_activity_log (
                log_id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                username VARCHAR(50) NOT NULL,
                action VARCHAR(20) NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // 6. Customer Requests Table (account closure requests, feedback, complaints)
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS customer_requests (
                request_id INT AUTO_INCREMENT PRIMARY KEY,
                customer_id INT NOT NULL,
                account_number BIGINT NULL,
                type VARCHAR(30) NOT NULL,
                subject VARCHAR(150),
                message TEXT,
                status VARCHAR(20) NOT NULL DEFAULT 'Pending',
                response TEXT,
                handled_by VARCHAR(50),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        // Seed initial admin and customer data if users table is empty
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("""
                    INSERT INTO users (user_id, username, password, role) VALUES
                    (1, 'headadmin', 'headadmin123', 'HeadAdmin'),
                    (2, 'admin', 'admin123', 'Admin'),
                    (3, 'san', 'user123', 'Customer'),
                    (4, 'jane_smith', 'user123', 'Customer')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO customers (customer_id, user_id, name, email, phone, address) VALUES
                    (101, 3, 'San', 'san@example.com', '+1-555-0192', '123 Main Street, New York, NY'),
                    (102, 4, 'Jane Smith', 'jane.smith@example.com', '+1-555-0148', '456 Oak Avenue, Los Angeles, CA')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO accounts (account_number, customer_id, balance, account_type, status, open_date) VALUES
                    (10001, 101, 5000.00, 'Savings', 'Active', '2025-01-15'),
                    (10002, 102, 2500.50, 'Current', 'Active', '2025-02-01')
                    """);

                stmt.executeUpdate("""
                    INSERT INTO transactions (account_number, type, amount, description) VALUES
                    (10001, 'DEPOSIT', 5000.00, 'Initial Account Opening Deposit'),
                    (10002, 'DEPOSIT', 2500.50, 'Initial Account Opening Deposit')
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
