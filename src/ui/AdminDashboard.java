package ui;

import classes.Account;
import classes.Admin;
import classes.Customer;
import classes.CustomerAccountDTO;
import classes.Transaction;
import db.DatabaseOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AdminDashboard extends JFrame {

    private final Admin admin;
    private final DatabaseOperations dao;

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

    // Tables
    private JTable customerTable;
    private DefaultTableModel customerTableModel;
    private JTable auditTable;
    private DefaultTableModel auditTableModel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        this.dao = new DatabaseOperations();
        initUI();
        loadCustomerData();
        loadAuditData();
    }

    private void initUI() {
        setTitle("Bank Management System - Admin Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        var mainPanel = new JPanel(new BorderLayout());

        // Header Panel
        var headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(26, 82, 118));
        headerPanel.setPreferredSize(new Dimension(950, 65));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        var titleLabel = new JLabel("ADMIN DASHBOARD");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        var welcomeLabel = new JLabel("Logged in as: " + admin.getUsername() + " (Admin ID: " + admin.getUserId() + ")");
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
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        var tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Register New Customer", createCustomerRegistrationPanel());
        tabbedPane.addTab("Customer Directory", createCustomerDirectoryPanel());
        tabbedPane.addTab("System Audit Log", createAuditLogPanel());

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 1) {
                loadCustomerData();
            } else if (selectedIndex == 2) {
                loadAuditData();
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

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

        // Row 3: Account Number & Initial Deposit
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Account Number:"), gbc);
        gbc.gridx = 1;
        accountNumberField = new JTextField(String.valueOf(generateAccountNumber()), 15);
        accountNumberField.setFont(fieldFont);
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

    private JPanel createCustomerDirectoryPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

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

    private JPanel createAuditLogPanel() {
        var panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

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
            if (initialDeposit < 0) {
                JOptionPane.showMessageDialog(this, "Initial deposit cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
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

    private void loadCustomerData() {
        customerTableModel.setRowCount(0);
        try {
            List<CustomerAccountDTO> list = dao.getAllCustomersWithAccounts();
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

    private void loadAuditData() {
        auditTableModel.setRowCount(0);
        try {
            List<Transaction> list = dao.getAllTransactions();
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

    private void clearRegistrationForm() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressArea.setText("");
        accountNumberField.setText(String.valueOf(generateAccountNumber()));
        initialDepositField.setText("1000.00");
    }

    private long generateAccountNumber() {
        return ThreadLocalRandom.current().nextLong(1000000000L, 10000000000L);
    }
}
