package db;

import classes.Account;
import classes.Admin;
import classes.AdminActivityDTO;
import classes.AdminDTO;
import classes.Customer;
import classes.CustomerAccountDTO;
import classes.HeadAdmin;
import classes.Transaction;
import classes.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseOperations {

    private final DBConfig dbConfig;

    public DatabaseOperations() {
        this.dbConfig = DBConfig.getInstance();
    }

    /**
     * Validates user credentials and returns the appropriate User subclass (Admin or Customer).
     */
    public User validateLogin(String username, String password, String role) throws SQLException {
        var sql = """
            SELECT u.user_id, u.username, u.password, u.role, c.customer_id, c.name, c.email, c.phone, c.address
            FROM users u
            LEFT JOIN customers c ON u.user_id = c.user_id
            WHERE u.username = ? AND u.password = ? AND u.role = ?
            """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            rs = stmt.executeQuery();
            if (rs.next()) {
                var userId = rs.getInt("user_id");
                var userRole = rs.getString("role");

                if ("HeadAdmin".equalsIgnoreCase(userRole)) {
                    var headAdmin = new HeadAdmin();
                    headAdmin.setUserId(userId);
                    headAdmin.setUsername(username);
                    headAdmin.setPassword(password);
                    headAdmin.setAdminId(userId);
                    recordAdminLogin(conn, userId, username);
                    return headAdmin;
                } else if ("Admin".equalsIgnoreCase(userRole)) {
                    var admin = new Admin();
                    admin.setUserId(userId);
                    admin.setUsername(username);
                    admin.setPassword(password);
                    admin.setAdminId(userId);
                    recordAdminLogin(conn, userId, username);
                    return admin;
                } else if ("Customer".equalsIgnoreCase(userRole)) {
                    var customer = new Customer();
                    customer.setUserId(userId);
                    customer.setUsername(username);
                    customer.setPassword(password);
                    customer.setCustomerId(rs.getInt("customer_id"));
                    customer.setName(rs.getString("name"));
                    customer.setEmail(rs.getString("email"));
                    customer.setPhone(rs.getString("phone"));
                    customer.setAddress(rs.getString("address"));
                    return customer;
                }
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Atomically creates a User, Customer profile, and Account within a single transaction.
     */
    public boolean createCustomerWithAccount(Customer customer, Account account) throws SQLException {
        var insertUserSql = """
            INSERT INTO users (username, password, role) VALUES (?, ?, 'Customer')
            """;
        var insertCustomerSql = """
            INSERT INTO customers (user_id, name, email, phone, address) VALUES (?, ?, ?, ?, ?)
            """;
        var insertAccountSql = """
            INSERT INTO accounts (account_number, customer_id, balance, account_type, status, open_date)
            VALUES (?, ?, ?, ?, 'Active', CURDATE())
            """;

        Connection conn = null;
        PreparedStatement userStmt = null;
        PreparedStatement custStmt = null;
        PreparedStatement accStmt = null;
        ResultSet rsKey = null;
        ResultSet rsCustKey = null;

        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert into users
            userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, customer.getUsername());
            userStmt.setString(2, customer.getPassword());
            int affectedUsers = userStmt.executeUpdate();
            if (affectedUsers == 0) {
                conn.rollback();
                return false;
            }

            rsKey = userStmt.getGeneratedKeys();
            int userId = 0;
            if (rsKey.next()) {
                userId = rsKey.getInt(1);
            } else {
                conn.rollback();
                return false;
            }

            // 2. Insert into customers
            custStmt = conn.prepareStatement(insertCustomerSql, Statement.RETURN_GENERATED_KEYS);
            custStmt.setInt(1, userId);
            custStmt.setString(2, customer.getName());
            custStmt.setString(3, customer.getEmail());
            custStmt.setString(4, customer.getPhone());
            custStmt.setString(5, customer.getAddress());
            int affectedCust = custStmt.executeUpdate();
            if (affectedCust == 0) {
                conn.rollback();
                return false;
            }

            rsCustKey = custStmt.getGeneratedKeys();
            int customerId = 0;
            if (rsCustKey.next()) {
                customerId = rsCustKey.getInt(1);
            } else {
                conn.rollback();
                return false;
            }

            customer.setUserId(userId);
            customer.setCustomerId(customerId);
            account.setCustomerId(customerId);

            // 3. Insert into accounts
            accStmt = conn.prepareStatement(insertAccountSql);
            accStmt.setLong(1, account.getAccountNumber());
            accStmt.setInt(2, customerId);
            accStmt.setDouble(3, account.getBalance());
            accStmt.setString(4, account.getAccountType());
            int affectedAcc = accStmt.executeUpdate();
            if (affectedAcc == 0) {
                conn.rollback();
                return false;
            }

            // 4. Initial deposit transaction entry if balance > 0
            if (account.getBalance() > 0) {
                var initTransSql = """
                    INSERT INTO transactions (account_number, type, amount, description)
                    VALUES (?, 'DEPOSIT', ?, 'Initial Account Opening Deposit')
                    """;
                try (var transStmt = conn.prepareStatement(initTransSql)) {
                    transStmt.setLong(1, account.getAccountNumber());
                    transStmt.setDouble(2, account.getBalance());
                    transStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw e;
        } finally {
            if (rsCustKey != null) try { rsCustKey.close(); } catch (SQLException ignored) {}
            if (rsKey != null) try { rsKey.close(); } catch (SQLException ignored) {}
            if (userStmt != null) try { userStmt.close(); } catch (SQLException ignored) {}
            if (custStmt != null) try { custStmt.close(); } catch (SQLException ignored) {}
            if (accStmt != null) try { accStmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Retrieves account details by account number.
     */
    public Account getAccountDetails(long accountNumber) throws SQLException {
        var sql = """
            SELECT account_number, customer_id, balance, account_type, status, open_date
            FROM accounts
            WHERE account_number = ?
            """;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, accountNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                var account = new Account();
                account.setAccountNumber(rs.getLong("account_number"));
                account.setCustomerId(rs.getInt("customer_id"));
                account.setBalance(rs.getDouble("balance"));
                account.setAccountType(rs.getString("account_type"));
                account.setStatus(rs.getString("status"));
                account.setOpenDate(rs.getDate("open_date"));
                return account;
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Retrieves account details by Customer ID.
     */
    public Account getAccountByCustomerId(int customerId) throws SQLException {
        var sql = """
            SELECT account_number, customer_id, balance, account_type, status, open_date
            FROM accounts
            WHERE customer_id = ? AND status = 'Active'
            LIMIT 1
            """;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                var account = new Account();
                account.setAccountNumber(rs.getLong("account_number"));
                account.setCustomerId(rs.getInt("customer_id"));
                account.setBalance(rs.getDouble("balance"));
                account.setAccountType(rs.getString("account_type"));
                account.setStatus(rs.getString("status"));
                account.setOpenDate(rs.getDate("open_date"));
                return account;
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return null;
    }

    /**
     * Deposits funds into an account and logs the transaction.
     */
    public boolean deposit(long accountNumber, double amount) throws SQLException {
        if (!Double.isFinite(amount) || amount <= 0) return false;

        var account = getAccountDetails(accountNumber);
        if (account == null || !"Active".equalsIgnoreCase(account.getStatus())) {
            return false;
        }

        var updateSql = """
            UPDATE accounts
            SET balance = balance + ?
            WHERE account_number = ? AND status = 'Active'
            """;
        var transSql = """
            INSERT INTO transactions (account_number, type, amount, description)
            VALUES (?, 'DEPOSIT', ?, 'Cash Deposit')
            """;

        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement transStmt = null;

        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);

            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setDouble(1, amount);
            updateStmt.setLong(2, accountNumber);
            int updated = updateStmt.executeUpdate();

            if (updated == 0) {
                conn.rollback();
                return false;
            }

            transStmt = conn.prepareStatement(transSql);
            transStmt.setLong(1, accountNumber);
            transStmt.setDouble(2, amount);
            transStmt.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            }
            throw e;
        } finally {
            if (updateStmt != null) try { updateStmt.close(); } catch (SQLException ignored) {}
            if (transStmt != null) try { transStmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Withdraws funds from an account after verifying sufficient balance.
     */
    public boolean withdraw(long accountNumber, double amount) throws SQLException {
        if (!Double.isFinite(amount) || amount <= 0) return false;

        var account = getAccountDetails(accountNumber);
        if (account == null || !"Active".equalsIgnoreCase(account.getStatus()) || account.getBalance() < amount) {
            return false;
        }

        var updateSql = """
            UPDATE accounts
            SET balance = balance - ?
            WHERE account_number = ? AND balance >= ? AND status = 'Active'
            """;
        var transSql = """
            INSERT INTO transactions (account_number, type, amount, description)
            VALUES (?, 'WITHDRAWAL', ?, 'Cash Withdrawal')
            """;

        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement transStmt = null;

        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);

            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setDouble(1, amount);
            updateStmt.setLong(2, accountNumber);
            updateStmt.setDouble(3, amount);
            int updated = updateStmt.executeUpdate();

            if (updated == 0) {
                conn.rollback();
                return false;
            }

            transStmt = conn.prepareStatement(transSql);
            transStmt.setLong(1, accountNumber);
            transStmt.setDouble(2, amount);
            transStmt.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            }
            throw e;
        } finally {
            if (updateStmt != null) try { updateStmt.close(); } catch (SQLException ignored) {}
            if (transStmt != null) try { transStmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Transfers funds atomically between source and target accounts with rollback handling.
     */
    public boolean transferFunds(long sourceAccount, long targetAccount, double amount) throws SQLException {
        if (!Double.isFinite(amount) || amount <= 0 || sourceAccount == targetAccount) return false;

        var src = getAccountDetails(sourceAccount);
        var tgt = getAccountDetails(targetAccount);

        if (src == null || !"Active".equalsIgnoreCase(src.getStatus()) || src.getBalance() < amount) {
            return false;
        }
        if (tgt == null || !"Active".equalsIgnoreCase(tgt.getStatus())) {
            return false;
        }

        var deductSql = """
            UPDATE accounts
            SET balance = balance - ?
            WHERE account_number = ? AND balance >= ? AND status = 'Active'
            """;
        var addSql = """
            UPDATE accounts
            SET balance = balance + ?
            WHERE account_number = ? AND status = 'Active'
            """;
        var transOut = """
            INSERT INTO transactions (account_number, type, amount, description)
            VALUES (?, 'TRANSFER', ?, ?)
            """;
        var transIn = """
            INSERT INTO transactions (account_number, type, amount, description)
            VALUES (?, 'TRANSFER', ?, ?)
            """;

        Connection conn = null;
        PreparedStatement deductStmt = null;
        PreparedStatement addStmt = null;
        PreparedStatement outStmt = null;
        PreparedStatement inStmt = null;

        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);

            deductStmt = conn.prepareStatement(deductSql);
            deductStmt.setDouble(1, amount);
            deductStmt.setLong(2, sourceAccount);
            deductStmt.setDouble(3, amount);

            addStmt = conn.prepareStatement(addSql);
            addStmt.setDouble(1, amount);
            addStmt.setLong(2, targetAccount);

            // Execute SQL statements in ascending order of account_number to prevent DB deadlocks
            if (sourceAccount < targetAccount) {
                int deductResult = deductStmt.executeUpdate();
                if (deductResult == 0) {
                    conn.rollback();
                    return false;
                }
                int addResult = addStmt.executeUpdate();
                if (addResult == 0) {
                    conn.rollback();
                    return false;
                }
            } else {
                int addResult = addStmt.executeUpdate();
                if (addResult == 0) {
                    conn.rollback();
                    return false;
                }
                int deductResult = deductStmt.executeUpdate();
                if (deductResult == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // Insert transaction log for source account
            outStmt = conn.prepareStatement(transOut);
            outStmt.setLong(1, sourceAccount);
            outStmt.setDouble(2, amount);
            outStmt.setString(3, "Fund Transfer Sent to Acc #" + targetAccount);
            outStmt.executeUpdate();

            // Insert transaction log for target account
            inStmt = conn.prepareStatement(transIn);
            inStmt.setLong(1, targetAccount);
            inStmt.setDouble(2, amount);
            inStmt.setString(3, "Fund Transfer Received from Acc #" + sourceAccount);
            inStmt.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw e;
        } finally {
            if (deductStmt != null) try { deductStmt.close(); } catch (SQLException ignored) {}
            if (addStmt != null) try { addStmt.close(); } catch (SQLException ignored) {}
            if (outStmt != null) try { outStmt.close(); } catch (SQLException ignored) {}
            if (inStmt != null) try { inStmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Gets transaction ledger history for a specific account.
     */
    public List<Transaction> getTransactionHistory(long accountNumber) throws SQLException {
        var list = new ArrayList<Transaction>();
        var sql = """
            SELECT transaction_id, account_number, type, amount, timestamp, description
            FROM transactions
            WHERE account_number = ?
            ORDER BY timestamp DESC
            """;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, accountNumber);
            rs = stmt.executeQuery();

            while (rs.next()) {
                var t = new Transaction();
                t.setTransactionId(rs.getInt("transaction_id"));
                t.setAccountNumber(rs.getLong("account_number"));
                t.setType(rs.getString("type"));
                t.setAmount(rs.getDouble("amount"));
                t.setTimestamp(rs.getTimestamp("timestamp"));
                t.setDescription(rs.getString("description"));
                list.add(t);
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Retrieves all customer profiles joined with their account details for Admin panel.
     */
    public List<CustomerAccountDTO> getAllCustomersWithAccounts() throws SQLException {
        return searchCustomers(null, null);
    }

    /**
     * Searches customers with an optional filter field ("All", "Name", "Account Number",
     * "Recent Transaction") and a free-text term.
     */
    public List<CustomerAccountDTO> searchCustomers(String field, String term) throws SQLException {
        var list = new ArrayList<CustomerAccountDTO>();
        var sql = new StringBuilder("""
            SELECT c.customer_id, c.user_id, c.name, c.email, c.phone, c.address,
                   a.account_number, a.balance, a.account_type, a.status
            FROM customers c
            LEFT JOIN accounts a ON c.customer_id = a.customer_id
            """);

        var params = new ArrayList<String>();
        if (term != null && !term.isBlank()) {
            String like = "%" + term.trim() + "%";
            switch (field == null ? "All" : field) {
                case "Name" -> {
                    sql.append(" WHERE c.name LIKE ? ");
                    params.add(like);
                }
                case "Account Number" -> {
                    sql.append(" WHERE CAST(a.account_number AS CHAR) LIKE ? ");
                    params.add(like);
                }
                case "Recent Transaction" -> {
                    sql.append(" WHERE c.customer_id IN (")
                       .append("SELECT a2.customer_id FROM accounts a2 ")
                       .append("JOIN transactions t ON t.account_number = a2.account_number ")
                       .append("WHERE t.description LIKE ? OR t.type LIKE ?) ");
                    params.add(like);
                    params.add(like);
                }
                default -> {
                    sql.append(" WHERE c.name LIKE ? OR c.email LIKE ? OR c.phone LIKE ? ")
                       .append("OR CAST(a.account_number AS CHAR) LIKE ? ");
                    params.add(like);
                    params.add(like);
                    params.add(like);
                    params.add(like);
                }
            }
        }
        sql.append(" ORDER BY c.customer_id DESC ");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new CustomerAccountDTO(
                        rs.getInt("customer_id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getLong("account_number"),
                        rs.getDouble("balance"),
                        rs.getString("account_type"),
                        rs.getString("status")
                ));
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Retrieves system-wide transaction audit records for Admin panel.
     */
    public List<Transaction> getAllTransactions() throws SQLException {
        return searchTransactions(null, null);
    }

    /**
     * Searches transactions with an optional filter field ("All", "Account Number",
     * "Type", "Description") and a free-text term.
     */
    public List<Transaction> searchTransactions(String field, String term) throws SQLException {
        var list = new ArrayList<Transaction>();
        var sql = new StringBuilder("""
            SELECT transaction_id, account_number, type, amount, timestamp, description
            FROM transactions
            """);

        var params = new ArrayList<String>();
        if (term != null && !term.isBlank()) {
            String like = "%" + term.trim() + "%";
            switch (field == null ? "All" : field) {
                case "Account Number" -> {
                    sql.append(" WHERE CAST(account_number AS CHAR) LIKE ? ");
                    params.add(like);
                }
                case "Type" -> {
                    sql.append(" WHERE type LIKE ? ");
                    params.add(like);
                }
                case "Description" -> {
                    sql.append(" WHERE description LIKE ? ");
                    params.add(like);
                }
                default -> {
                    sql.append(" WHERE CAST(account_number AS CHAR) LIKE ? OR type LIKE ? ")
                       .append("OR description LIKE ? ");
                    params.add(like);
                    params.add(like);
                    params.add(like);
                }
            }
        }
        sql.append(" ORDER BY timestamp DESC ");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                var t = new Transaction();
                t.setTransactionId(rs.getInt("transaction_id"));
                t.setAccountNumber(rs.getLong("account_number"));
                t.setType(rs.getString("type"));
                t.setAmount(rs.getDouble("amount"));
                t.setTimestamp(rs.getTimestamp("timestamp"));
                t.setDescription(rs.getString("description"));
                list.add(t);
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Admin management (Head Admin console)
    // ------------------------------------------------------------------

    /**
     * Returns every Admin account with its online status and last login time.
     */
    public List<AdminDTO> getAllAdmins() throws SQLException {
        var list = new ArrayList<AdminDTO>();
        var sql = """
            SELECT user_id, username, is_online, last_login
            FROM users
            WHERE role = 'Admin'
            ORDER BY user_id
            """;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new AdminDTO(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getBoolean("is_online"),
                        rs.getTimestamp("last_login")
                ));
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Creates a new Admin account.
     */
    public boolean createAdmin(String username, String password) throws SQLException {
        var sql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'Admin')";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet keys = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, password);
            if (stmt.executeUpdate() == 0) {
                return false;
            }
            keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                logAdminActivity(conn, keys.getInt(1), username, "CREATED");
            }
            return true;
        } finally {
            if (keys != null) try { keys.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Forces an Admin offline and records the event.
     */
    public boolean forceLogoutAdmin(int userId, String username) throws SQLException {
        var updateSql = "UPDATE users SET is_online = 0 WHERE user_id = ? AND role = 'Admin'";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, userId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) {
                logAdminActivity(conn, userId, username, "FORCE_LOGOUT");
            }
            return updated;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Deletes an Admin account.
     */
    public boolean removeAdmin(int userId) throws SQLException {
        var sql = "DELETE FROM users WHERE user_id = ? AND role = 'Admin'";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    /**
     * Returns the admin activity audit trail, newest first.
     */
    public List<AdminActivityDTO> getAdminActivityLog() throws SQLException {
        var list = new ArrayList<AdminActivityDTO>();
        var sql = """
            SELECT log_id, username, action, timestamp
            FROM admin_activity_log
            ORDER BY timestamp DESC
            """;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new AdminActivityDTO(
                        rs.getInt("log_id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getTimestamp("timestamp")
                ));
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return list;
    }

    /**
     * Returns the next sequential account number (highest existing + 1, starting at 1000000001).
     */
    public long getNextAccountNumber() throws SQLException {
        var sql = "SELECT COALESCE(MAX(account_number), 1000000000) + 1 FROM accounts";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } finally {
            DBConfig.closeResources(rs, stmt, conn);
        }
        return 1000000001L;
    }

    /**
     * Marks an admin online and records a LOGIN event (called on successful admin login).
     */
    private void recordAdminLogin(Connection conn, int userId, String username) throws SQLException {
        try (var ps = conn.prepareStatement(
                "UPDATE users SET is_online = 1, last_login = NOW() WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        logAdminActivity(conn, userId, username, "LOGIN");
    }

    /**
     * Marks an admin offline and records a LOGOUT event.
     */
    public void recordLogout(int userId, String username) {
        if (userId <= 0) return;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dbConfig.getConnection();
            stmt = conn.prepareStatement("UPDATE users SET is_online = 0 WHERE user_id = ?");
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close();
            stmt = null;
            logAdminActivity(conn, userId, username, "LOGOUT");
        } catch (SQLException e) {
            System.err.println("Error recording logout: " + e.getMessage());
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            DBConfig.closeConnection(conn);
        }
    }

    private void logAdminActivity(Connection conn, int userId, String username, String action) throws SQLException {
        try (var ps = conn.prepareStatement(
                "INSERT INTO admin_activity_log (user_id, username, action) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.executeUpdate();
        }
    }
}
