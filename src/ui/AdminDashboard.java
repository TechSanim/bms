package ui;

import classes.Account;
import classes.Admin;
import classes.AdminActivityDTO;
import classes.AdminDTO;
import classes.Customer;
import classes.CustomerAccountDTO;
import classes.CustomerRequestDTO;
import classes.HeadAdmin;
import classes.Transaction;
import db.DatabaseOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class AdminDashboard extends JFrame {

    private final Admin admin;
    private final DatabaseOperations dao;
    private final boolean headAdmin;

    // Components for Customer Registration Tab
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextArea addressArea;
    private JTextField accountNumberField;
    private JTextField initialDepositField;
    private JComboBox<String> accountTypeCombo;

    // Customer Directory Tab
    private JTable customerTable;
    private DefaultTableModel customerTableModel;
    private JTextField customerSearchField;
    private JComboBox<String> customerFilterCombo;

    // System Audit Log Tab
    private JTable auditTable;
    private DefaultTableModel auditTableModel;
    private JTextField auditSearchField;
    private JComboBox<String> auditFilterCombo;

    // Manage Admins Tab (Head Admin only)
    private JTable adminTable;
    private DefaultTableModel adminTableModel;

    // Admin Activity Log Tab (Head Admin only)
    private JTable adminActivityTable;
    private DefaultTableModel adminActivityTableModel;

    // Customer Requests Tab
    private JTable requestTable;
    private DefaultTableModel requestTableModel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        this.dao = new DatabaseOperations();
        this.headAdmin = admin instanceof HeadAdmin;
        initUI();
        loadCustomerData();
        loadAuditData();
        loadCustomerRequests();
        if (headAdmin) {
            loadAdminData();
            loadAdminActivityData();
        }
    }

    private void initUI() {
        setTitle(headAdmin
                ? "Bank Management System - Head Admin Control Panel"
                : "Bank Management System - Admin Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);

        var mainPanel = new JPanel(new BorderLayout());

        // Header Panel
        var headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(26, 82, 118));
        headerPanel.setPreferredSize(new Dimension(980, 65));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        var titleLabel = new JLabel(headAdmin ? "HEAD ADMIN DASHBOARD" : "ADMIN DASHBOARD");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        var welcomeLabel = new JLabel("Logged in as: " + admin.getUsername()
                + " (" + (headAdmin ? "Head Admin" : "Admin") + " ID: " + admin.getUserId() + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeLabel.setForeground(new Color(212, 230, 241));

        var headerTextPanel = new JPanel(new GridLayout(2, 1));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(welcomeLabel);

        var logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutButton.setBackground(new Color(192, 57, 43));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.addActionListener(e -> handleLogout());

        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        var tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        if (headAdmin) {
            tabbedPane.addTab("Manage Admins", createAdminManagementPanel());
            tabbedPane.addTab("Admin Activity Log", createAdminActivityPanel());
        }
        tabbedPane.addTab("Register New Customer", createCustomerRegistrationPanel());
        tabbedPane.addTab("Customer Directory", createCustomerDirectoryPanel());
        tabbedPane.addTab("System Audit Log", createAuditLogPanel());
        tabbedPane.addTab("Customer Requests", createCustomerRequestsPanel());

        tabbedPane.addChangeListener(e -> {
            String title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            switch (title) {
                case "Manage Admins" -> loadAdminData();
                case "Admin Activity Log" -> loadAdminActivityData();
                case "Customer Directory" -> loadCustomerData();
                case "System Audit Log" -> loadAuditData();
                case "Customer Requests" -> loadCustomerRequests();
                default -> { }
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void handleLogout() {
        dao.recordLogout(admin.getUserId(), admin.getUsername());
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ------------------------------------------------------------------
    // Register New Customer
    // ------------------------------------------------------------------

    private JPanel createCustomerRegistrationPanel() {
        var panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(15, 30, 15, 30));
        panel.setBackground(new Color(245, 247, 250));

        var formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        var fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Row 0: Username & Password
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        usernameField.setFont(fieldFont);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 3;
        passwordField = new JPasswordField(15);
        passwordField.setFont(fieldFont);
        passwordField.setToolTipText("Min 8 characters with an uppercase letter, lowercase letter, number, and symbol.");
        formPanel.add(passwordField, gbc);

        // Row 1: Full Name & Email
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(fieldFont);
        formPanel.add(nameField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Email Address:"), gbc);
        gbc.gridx = 3;
        emailField = new JTextField(15);
        emailField.setFont(fieldFont);
        formPanel.add(emailField, gbc);

        // Row 2: Phone & Account Type
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(fieldFont);
        phoneField.setToolTipText("Enter a 10-digit phone number.");
        ((AbstractDocument) phoneField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                int currentLength = fb.getDocument().getLength();
                if (string.matches("\\d*") && currentLength + string.length() <= 10) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                int currentLength = fb.getDocument().getLength();
                int newLength = currentLength - length + (text == null ? 0 : text.length());
                if ((text == null || text.matches("\\d*")) && newLength <= 10) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        formPanel.add(phoneField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Account Type:"), gbc);
        gbc.gridx = 3;
        accountTypeCombo = new JComboBox<>(new String[]{"Savings", "Current"});
        accountTypeCombo.setFont(fieldFont);
        formPanel.add(accountTypeCombo, gbc);

        // Row 3: Account Number (auto, read-only) & Initial Deposit
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Account Number:"), gbc);
        gbc.gridx = 1;
        accountNumberField = new JTextField(nextAccountNumber(), 15);
        accountNumberField.setFont(fieldFont);
        accountNumberField.setEditable(false);
        accountNumberField.setBackground(new Color(235, 235, 235));
        formPanel.add(accountNumberField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Initial Deposit ($):"), gbc);
        gbc.gridx = 3;
        initialDepositField = new JTextField("1000.00", 15);
        initialDepositField.setFont(fieldFont);
        formPanel.add(initialDepositField, gbc);

        // Row 4: Address
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        addressArea = new JTextArea(3, 40);
        addressArea.setFont(fieldFont);
        addressArea.setLineWrap(true);
        var addressScroll = new JScrollPane(addressArea);
        formPanel.add(addressScroll, gbc);

        // Row 5: Action Button
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4;
        gbc.insets = new Insets(20, 12, 10, 12);

        var registerButton = new JButton("Register Customer & Create Account");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setBackground(new Color(40, 116, 166));
        registerButton.setForeground(Color.WHITE);
        registerButton.setPreferredSize(new Dimension(320, 40));
        registerButton.setFocusPainted(false);
        registerButton.addActionListener(e -> handleRegisterCustomer());

        var btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnWrapper.setOpaque(false);
        btnWrapper.add(registerButton);

        formPanel.add(btnWrapper, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    private void handleRegisterCustomer() {
        var username = usernameField.getText().trim();
        var password = new String(passwordField.getPassword());
        var name = nameField.getText().trim();
        var email = emailField.getText().trim();
        var phone = phoneField.getText().trim();
        var address = addressArea.getText().trim();
        var accNumStr = accountNumberField.getText().trim();
        var depositStr = initialDepositField.getText().trim();
        var accountType = (String) accountTypeCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || address.isEmpty() || accNumStr.isEmpty() || depositStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (Username, Password, Name, Email, Phone, Address, Account #, Deposit).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) {
            JOptionPane.showMessageDialog(this, passwordError, "Invalid Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Username cannot exceed 50 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (name.length() > 100) {
            JOptionPane.showMessageDialog(this, "Full Name cannot exceed 100 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (email.length() > 100) {
            JOptionPane.showMessageDialog(this, "Email Address cannot exceed 100 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long accountNumber;
        double initialDeposit;
        try {
            accountNumber = Long.parseLong(accNumStr);
            if (accountNumber <= 0) {
                JOptionPane.showMessageDialog(this, "Account Number must be a positive number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            initialDeposit = Double.parseDouble(depositStr);
            if (!Double.isFinite(initialDeposit) || initialDeposit < 1000) {
                JOptionPane.showMessageDialog(this, "Initial deposit must be at least $1000.00.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric Account Number and Initial Deposit amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        var customer = new Customer(username, password, name, email, phone, address);
        var account = new Account(accountNumber, 0, initialDeposit, accountType);

        try {
            boolean success = dao.createCustomerWithAccount(customer, account);
            if (success) {
                JOptionPane.showMessageDialog(this, "Customer and Account created successfully!\nAccount Number: " + accountNumber, "Success", JOptionPane.INFORMATION_MESSAGE);
                clearRegistrationForm();
                loadCustomerData();
                loadAuditData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to register customer. Username or Account Number may already exist.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSymbol = true;
            }
        }
        if (!hasUpper || !hasLower || !hasDigit || !hasSymbol) {
            return "Password must contain at least one uppercase letter, one lowercase letter, one number, and one symbol.";
        }
        return null;
    }

    private void clearRegistrationForm() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressArea.setText("");
        accountNumberField.setText(nextAccountNumber());
        initialDepositField.setText("1000.00");
    }

    private String nextAccountNumber() {
        try {
            return String.valueOf(dao.getNextAccountNumber());
        } catch (SQLException e) {
            return "10001";
        }
    }

    private JPanel createSearchPanel(JComboBox<String> filterCombo, JTextField searchField, Runnable refreshAction) {
        var searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.add(new JLabel("Filter by:"));

        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(filterCombo);

        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.addActionListener(e -> refreshAction.run());
        searchPanel.add(searchField);

        var searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.addActionListener(e -> refreshAction.run());
        searchPanel.add(searchButton);

        var clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> {
            searchField.setText("");
            filterCombo.setSelectedIndex(0);
            refreshAction.run();
        });
        searchPanel.add(clearButton);

        return searchPanel;
    }

    // ------------------------------------------------------------------
    // Customer Directory
    // ------------------------------------------------------------------

    private JPanel createCustomerDirectoryPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        customerFilterCombo = new JComboBox<>(new String[]{"All", "Name", "Account Number", "Recent Transaction"});
        customerSearchField = new JTextField(22);
        panel.add(createSearchPanel(customerFilterCombo, customerSearchField, this::loadCustomerData), BorderLayout.NORTH);

        String[] columns = {"Cust ID", "User ID", "Name", "Email", "Phone", "Account #", "Type", "Balance ($)", "Status"};
        customerTableModel = UiUtils.readOnlyModel(columns);
        customerTable = UiUtils.createTable(customerTableModel);

        var scrollPane = new JScrollPane(customerTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        var refreshButton = new JButton("Refresh Directory");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshButton.addActionListener(e -> loadCustomerData());

        var southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(refreshButton);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadCustomerData() {
        if (customerTableModel == null) return;
        customerTableModel.setRowCount(0);
        try {
            String field = customerFilterCombo == null ? null : (String) customerFilterCombo.getSelectedItem();
            String term = customerSearchField == null ? null : customerSearchField.getText();
            List<CustomerAccountDTO> list = dao.searchCustomers(field, term);
            for (CustomerAccountDTO dto : list) {
                Object[] row = {
                        dto.customerId(),
                        dto.userId(),
                        dto.name(),
                        dto.email(),
                        dto.phone(),
                        dto.accountNumber() > 0 ? dto.accountNumber() : "N/A",
                        dto.accountType() != null ? dto.accountType() : "N/A",
                        dto.accountNumber() > 0 ? String.format("%.2f", dto.balance()) : "N/A",
                        dto.status() != null ? dto.status() : "N/A"
                };
                customerTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading customers: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // System Audit Log
    // ------------------------------------------------------------------

    private JPanel createAuditLogPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        auditFilterCombo = new JComboBox<>(new String[]{"All", "Account Number", "Type", "Description"});
        auditSearchField = new JTextField(22);
        panel.add(createSearchPanel(auditFilterCombo, auditSearchField, this::loadAuditData), BorderLayout.NORTH);

        String[] columns = {"Tx ID", "Account #", "Type", "Amount ($)", "Timestamp", "Description"};
        auditTableModel = UiUtils.readOnlyModel(columns);
        auditTable = UiUtils.createTable(auditTableModel);

        var scrollPane = new JScrollPane(auditTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        var refreshAuditButton = new JButton("Refresh Audit Ledger");
        refreshAuditButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshAuditButton.addActionListener(e -> loadAuditData());

        var southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(refreshAuditButton);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAuditData() {
        if (auditTableModel == null) return;
        auditTableModel.setRowCount(0);
        try {
            String field = auditFilterCombo == null ? null : (String) auditFilterCombo.getSelectedItem();
            String term = auditSearchField == null ? null : auditSearchField.getText();
            List<Transaction> list = dao.searchTransactions(field, term);
            for (Transaction t : list) {
                Object[] row = {
                        t.getTransactionId(),
                        t.getAccountNumber(),
                        t.getType(),
                        String.format("%.2f", t.getAmount()),
                        t.getTimestamp(),
                        t.getDescription()
                };
                auditTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading audit log: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Manage Admins (Head Admin only)
    // ------------------------------------------------------------------

    private JPanel createAdminManagementPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Admin ID", "Username", "Status", "Last Login"};
        adminTableModel = UiUtils.readOnlyModel(columns);
        adminTable = UiUtils.createTable(adminTableModel);

        panel.add(new JScrollPane(adminTable), BorderLayout.CENTER);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        var addButton = new JButton("Add New Admin");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addButton.setBackground(new Color(40, 116, 166));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> showAddAdminDialog());

        var forceLogoutButton = new JButton("Force Logout");
        forceLogoutButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        forceLogoutButton.addActionListener(e -> forceLogoutSelectedAdmin());

        var removeButton = new JButton("Remove Admin");
        removeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        removeButton.addActionListener(e -> removeSelectedAdmin());

        var refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.addActionListener(e -> loadAdminData());

        buttonPanel.add(addButton);
        buttonPanel.add(forceLogoutButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAdminData() {
        if (adminTableModel == null) return;
        adminTableModel.setRowCount(0);
        try {
            for (AdminDTO a : dao.getAllAdmins()) {
                Object[] row = {
                        a.userId(),
                        a.username(),
                        a.online() ? "Online" : "Offline",
                        a.lastLogin() != null ? a.lastLogin().toString() : "Never"
                };
                adminTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading admins: " + e.getMessage());
        }
    }

    private void showAddAdminDialog() {
        var panel = new JPanel(new GridLayout(2, 2, 8, 8));
        var userField = new JTextField();
        var passField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Admin",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and Password are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            JOptionPane.showMessageDialog(this, passwordError, "Invalid Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Username cannot exceed 50 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (dao.createAdmin(username, password)) {
                JOptionPane.showMessageDialog(this, "Admin '" + username + "' created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAdminData();
                loadAdminActivityData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create admin.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not create admin. The username may already exist.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void forceLogoutSelectedAdmin() {
        int row = adminTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an admin from the list first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) adminTableModel.getValueAt(row, 0);
        String username = (String) adminTableModel.getValueAt(row, 1);
        String status = (String) adminTableModel.getValueAt(row, 2);

        if (!"Online".equals(status)) {
            JOptionPane.showMessageDialog(this, "Admin '" + username + "' is already offline.", "Already Offline", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Force logout admin '" + username + "'?", "Confirm Force Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (dao.forceLogoutAdmin(userId, username)) {
                loadAdminData();
                loadAdminActivityData();
            } else {
                JOptionPane.showMessageDialog(this, "Could not force logout that admin.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedAdmin() {
        int row = adminTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an admin from the list first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) adminTableModel.getValueAt(row, 0);
        String username = (String) adminTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently remove admin '" + username + "'?", "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (dao.removeAdmin(userId)) {
                loadAdminData();
                loadAdminActivityData();
            } else {
                JOptionPane.showMessageDialog(this, "Could not remove that admin.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Admin Activity Log (Head Admin only)
    // ------------------------------------------------------------------

    private JPanel createAdminActivityPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Log ID", "Username", "Action", "Timestamp"};
        adminActivityTableModel = UiUtils.readOnlyModel(columns);
        adminActivityTable = UiUtils.createTable(adminActivityTableModel);

        panel.add(new JScrollPane(adminActivityTable), BorderLayout.CENTER);

        var refreshButton = new JButton("Refresh Activity Log");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshButton.addActionListener(e -> loadAdminActivityData());

        var southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(refreshButton);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAdminActivityData() {
        if (adminActivityTableModel == null) return;
        adminActivityTableModel.setRowCount(0);
        try {
            for (AdminActivityDTO log : dao.getAdminActivityLog()) {
                Object[] row = {
                        log.logId(),
                        log.username(),
                        log.action(),
                        log.timestamp()
                };
                adminActivityTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading admin activity: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Customer Requests
    // ------------------------------------------------------------------

    private JPanel createCustomerRequestsPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Req ID", "Customer", "Acct #", "Type", "Subject", "Message", "Status", "Response", "Submitted"};
        requestTableModel = UiUtils.readOnlyModel(columns);
        requestTable = UiUtils.createTable(requestTableModel);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        requestTable.getColumnModel().getColumn(5).setPreferredWidth(260);
        requestTable.getColumnModel().getColumn(7).setPreferredWidth(200);
        requestTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int row = requestTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        requestTable.setRowSelectionInterval(row, row);
                        showRequestDetails(row);
                    }
                }
            }
        });

        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        var refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.addActionListener(e -> loadCustomerRequests());

        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadCustomerRequests() {
        if (requestTableModel == null) return;
        requestTableModel.setRowCount(0);
        try {
            for (CustomerRequestDTO r : dao.getAllCustomerRequests()) {
                Object[] row = {
                        r.requestId(),
                        r.customerName(),
                        r.accountNumber() != null ? r.accountNumber() : "—",
                        r.type(),
                        r.subject(),
                        r.message(),
                        r.status(),
                        r.response() != null ? r.response() : "",
                        r.createdAt()
                };
                requestTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading customer requests: " + e.getMessage());
        }
    }

    private void showRequestDetails(int row) {
        int requestId = (int) requestTableModel.getValueAt(row, 0);
        String customer = String.valueOf(requestTableModel.getValueAt(row, 1));
        Object acctObj = requestTableModel.getValueAt(row, 2);
        String type = String.valueOf(requestTableModel.getValueAt(row, 3));
        String subject = String.valueOf(requestTableModel.getValueAt(row, 4));
        String message = String.valueOf(requestTableModel.getValueAt(row, 5));
        String status = String.valueOf(requestTableModel.getValueAt(row, 6));
        String response = String.valueOf(requestTableModel.getValueAt(row, 7));
        String submitted = String.valueOf(requestTableModel.getValueAt(row, 8));

        boolean isClosure = "Account Closure".equals(type);
        boolean pending = "Pending".equals(status);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Segoe UI; font-size:12px;'>");
        html.append("<b>Request ID:</b> ").append(requestId).append("<br>");
        html.append("<b>Customer:</b> ").append(UiUtils.escapeHtml(customer)).append("<br>");
        html.append("<b>Account #:</b> ").append(UiUtils.escapeHtml(String.valueOf(acctObj))).append("<br>");
        html.append("<b>Type:</b> ").append(UiUtils.escapeHtml(type)).append("<br>");
        html.append("<b>Status:</b> ").append(UiUtils.escapeHtml(status)).append("<br>");
        html.append("<b>Submitted:</b> ").append(UiUtils.escapeHtml(submitted)).append("<br><br>");
        html.append("<b>Subject:</b><br>").append(UiUtils.escapeHtml(subject)).append("<br><br>");
        html.append("<b>Message:</b><br>").append(UiUtils.escapeHtml(message).replace("\n", "<br>"));
        if (response != null && !response.isEmpty() && !"null".equals(response)) {
            html.append("<br><br><b>Admin Response:</b><br>")
                    .append(UiUtils.escapeHtml(response).replace("\n", "<br>"));
        }
        html.append("</body></html>");

        JTextPane detailsPane = new JTextPane();
        detailsPane.setContentType("text/html");
        detailsPane.setText(html.toString());
        detailsPane.setEditable(false);
        detailsPane.setCaretPosition(0);
        detailsPane.setBackground(new Color(250, 250, 250));

        JScrollPane scroll = new JScrollPane(detailsPane);
        scroll.setPreferredSize(new Dimension(540, 380));

        if (!pending) {
            JOptionPane.showMessageDialog(this, scroll, "Request #" + requestId + " - " + type, JOptionPane.PLAIN_MESSAGE);
            return;
        }

        if (isClosure) {
            Object[] options = {"Approve & Close Account", "Reject", "Close"};
            int choice = JOptionPane.showOptionDialog(this, scroll,
                    "Request #" + requestId + " - " + type,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                if (acctObj instanceof Long) {
                    approveClosure((Long) acctObj, requestId);
                } else {
                    JOptionPane.showMessageDialog(this, "This request has no linked account.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (choice == 1) {
                rejectRequest(requestId);
            }
            return;
        }

        showReplyDialog(requestId, type, scroll);
    }

    private void showReplyDialog(int requestId, String type, JScrollPane detailsScroll) {
        JTextArea replyArea = new JTextArea(4, 40);
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel replyLabel = new JLabel("Reply:");
        replyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel replyPanel = new JPanel(new BorderLayout(0, 4));
        replyPanel.add(replyLabel, BorderLayout.NORTH);
        replyPanel.add(new JScrollPane(replyArea), BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setEnabled(false);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JDialog dialog = new JDialog(this, "Request #" + requestId + " - " + type, true);
        sendButton.addActionListener(e -> {
            String reply = replyArea.getText().trim();
            if (reply.isEmpty()) return;
            try {
                if (dao.updateRequestStatus(requestId, "Resolved", reply, admin.getUsername())) {
                    JOptionPane.showMessageDialog(dialog, "Reply sent.", "Resolved", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomerRequests();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Could not update the request.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        closeButton.addActionListener(e -> dialog.dispose());

        replyArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateSendState() {
                sendButton.setEnabled(!replyArea.getText().trim().isEmpty());
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSendState();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSendState();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSendState();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonPanel.add(sendButton);
        buttonPanel.add(closeButton);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        content.add(detailsScroll, BorderLayout.CENTER);
        content.add(replyPanel, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(content, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void approveClosure(long accountNumber, int requestId) {
        var responseField = new JTextField("Account closure approved.", 30);
        var dialogPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        dialogPanel.add(new JLabel("Close account #" + accountNumber + "? Optional note to customer:"));
        dialogPanel.add(responseField);

        int confirm = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Approve Account Closure", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        String response = responseField.getText().trim();
        if (response.isEmpty()) response = "Account closure approved.";

        try {
            if (dao.approveAccountClosure(requestId, accountNumber, response, admin.getUsername())) {
                JOptionPane.showMessageDialog(this, "Account #" + accountNumber + " has been closed and the customer can no longer log in.", "Account Closed", JOptionPane.INFORMATION_MESSAGE);
                loadCustomerRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Could not close the account.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectRequest(int requestId) {
        String reason = JOptionPane.showInputDialog(this, "Reason for rejecting this request:", "Reject Request", JOptionPane.PLAIN_MESSAGE);
        if (reason == null) return;
        reason = reason.trim();
        if (reason.isEmpty()) reason = "Request rejected.";

        try {
            if (dao.updateRequestStatus(requestId, "Rejected", reason, admin.getUsername())) {
                loadCustomerRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Could not update the request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
