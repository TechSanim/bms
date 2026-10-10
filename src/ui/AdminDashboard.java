package ui;

import classes.Account;
import classes.Admin;
import classes.AdminActivityDTO;
import classes.AdminDTO;
import classes.Customer;
import classes.CustomerAccountDTO;
import classes.HeadAdmin;
import classes.Transaction;
import db.DatabaseOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
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

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        this.dao = new DatabaseOperations();
        this.headAdmin = admin instanceof HeadAdmin;
        initUI();
        loadCustomerData();
        loadAuditData();
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

        tabbedPane.addChangeListener(e -> {
            String title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            switch (title) {
                case "Manage Admins" -> loadAdminData();
                case "Admin Activity Log" -> loadAdminActivityData();
                case "Customer Directory" -> loadCustomerData();
                case "System Audit Log" -> loadAuditData();
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

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty() || accNumStr.isEmpty() || depositStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (Username, Password, Name, Email, Phone, Account #, Deposit).", "Validation Error", JOptionPane.WARNING_MESSAGE);
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

        if (phone.length() > 20) {
            JOptionPane.showMessageDialog(this, "Phone number cannot exceed 20 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
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
            if (!Double.isFinite(initialDeposit) || initialDeposit < 0) {
                JOptionPane.showMessageDialog(this, "Initial deposit must be a valid non-negative number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
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
            return "1000000001";
        }
    }

    // ------------------------------------------------------------------
    // Customer Directory
    // ------------------------------------------------------------------

    private JPanel createCustomerDirectoryPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        var searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.add(new JLabel("Filter by:"));
        customerFilterCombo = new JComboBox<>(new String[]{"All", "Name", "Account Number", "Recent Transaction"});
        customerFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(customerFilterCombo);

        customerSearchField = new JTextField(22);
        customerSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerSearchField.addActionListener(e -> loadCustomerData());
        searchPanel.add(customerSearchField);

        var searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.addActionListener(e -> loadCustomerData());
        searchPanel.add(searchButton);

        var clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> {
            customerSearchField.setText("");
            customerFilterCombo.setSelectedIndex(0);
            loadCustomerData();
        });
        searchPanel.add(clearButton);

        panel.add(searchPanel, BorderLayout.NORTH);

        String[] columns = {"Cust ID", "User ID", "Name", "Email", "Phone", "Account #", "Type", "Balance ($)", "Status"};
        customerTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(customerTableModel);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerTable.setRowHeight(24);
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        customerTable.getTableHeader().setBackground(new Color(230, 235, 240));

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

        var searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.add(new JLabel("Filter by:"));
        auditFilterCombo = new JComboBox<>(new String[]{"All", "Account Number", "Type", "Description"});
        auditFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(auditFilterCombo);

        auditSearchField = new JTextField(22);
        auditSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        auditSearchField.addActionListener(e -> loadAuditData());
        searchPanel.add(auditSearchField);

        var searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.addActionListener(e -> loadAuditData());
        searchPanel.add(searchButton);

        var clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> {
            auditSearchField.setText("");
            auditFilterCombo.setSelectedIndex(0);
            loadAuditData();
        });
        searchPanel.add(clearButton);

        panel.add(searchPanel, BorderLayout.NORTH);

        String[] columns = {"Tx ID", "Account #", "Type", "Amount ($)", "Timestamp", "Description"};
        auditTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        auditTable = new JTable(auditTableModel);
        auditTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        auditTable.setRowHeight(24);
        auditTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        auditTable.getTableHeader().setBackground(new Color(230, 235, 240));

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
        adminTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        adminTable = new JTable(adminTableModel);
        adminTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        adminTable.setRowHeight(24);
        adminTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        adminTable.getTableHeader().setBackground(new Color(230, 235, 240));

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
        adminActivityTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        adminActivityTable = new JTable(adminActivityTableModel);
        adminActivityTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        adminActivityTable.setRowHeight(24);
        adminActivityTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        adminActivityTable.getTableHeader().setBackground(new Color(230, 235, 240));

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
}
