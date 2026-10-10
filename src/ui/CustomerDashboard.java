package ui;

import classes.Account;
import classes.Customer;
import classes.CustomerRequestDTO;
import classes.Transaction;
import db.DatabaseOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CustomerDashboard extends JFrame {

    private final Customer customer;
    private final DatabaseOperations dao;
    private Account customerAccount;

    // Account summary
    private JLabel balanceValueLabel;
    private JLabel accountInfoLabel;

    // Transaction history
    private JTable transactionTable;
    private DefaultTableModel tableModel;

    // Support & messages
    private JComboBox<String> requestTypeCombo;
    private JTextField subjectField;
    private JTextArea messageArea;
    private JTable requestTable;
    private DefaultTableModel requestTableModel;

    public CustomerDashboard(Customer customer) {
        this.customer = customer;
        this.dao = new DatabaseOperations();
        loadCustomerAccount();
        initUI();
        loadTransactionHistory();
        loadMyRequests();
    }

    private void loadCustomerAccount() {
        try {
            this.customerAccount = dao.getAccountByCustomerId(customer.getCustomerId());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching account details: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Bank Management System - Customer Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(940, 640);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(21, 67, 96));
        headerPanel.setPreferredSize(new Dimension(940, 65));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("CUSTOMER BANKING PORTAL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel welcomeLabel = new JLabel("Welcome back, " + customer.getName() + " (" + customer.getEmail() + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        welcomeLabel.setForeground(new Color(212, 230, 241));

        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(welcomeLabel);

        JButton logoutButton = new JButton("Logout");
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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("My Account", createAccountPanel());
        tabbedPane.addTab("Support & Messages", createSupportPanel());
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                loadMyRequests();
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    // ------------------------------------------------------------------
    // My Account
    // ------------------------------------------------------------------

    private JPanel createAccountPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        centerPanel.setBackground(new Color(245, 247, 250));

        // Account Summary Banner Card
        JPanel summaryCard = new JPanel(new BorderLayout());
        summaryCard.setBackground(new Color(40, 116, 166));
        summaryCard.setBorder(new EmptyBorder(15, 25, 15, 25));

        long accNum = (customerAccount != null) ? customerAccount.getAccountNumber() : 0;
        String accType = (customerAccount != null) ? customerAccount.getAccountType() : "N/A";
        String accStatus = (customerAccount != null) ? customerAccount.getStatus() : "N/A";
        double balance = (customerAccount != null) ? customerAccount.getBalance() : 0.0;

        accountInfoLabel = new JLabel(String.format("Account #: %d  |  Type: %s  |  Status: %s", accNum, accType, accStatus));
        accountInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        accountInfoLabel.setForeground(new Color(235, 245, 251));

        JLabel balanceTitleLabel = new JLabel("CURRENT AVAILABLE BALANCE");
        balanceTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        balanceTitleLabel.setForeground(new Color(212, 230, 241));

        balanceValueLabel = new JLabel(String.format("$%,.2f", balance));
        balanceValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        balanceValueLabel.setForeground(Color.WHITE);

        JPanel balanceTextPanel = new JPanel(new GridLayout(2, 1));
        balanceTextPanel.setOpaque(false);
        balanceTextPanel.add(balanceTitleLabel);
        balanceTextPanel.add(balanceValueLabel);

        summaryCard.add(accountInfoLabel, BorderLayout.NORTH);
        summaryCard.add(balanceTextPanel, BorderLayout.CENTER);

        centerPanel.add(summaryCard, BorderLayout.NORTH);

        // Action Controls Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        actionPanel.setOpaque(false);

        JButton depositBtn = createActionButton("Deposit Funds", new Color(39, 174, 96));
        JButton withdrawBtn = createActionButton("Withdraw Cash", new Color(211, 84, 0));
        JButton transferBtn = createActionButton("Transfer Funds", new Color(41, 128, 185));
        JButton closeBtn = createActionButton("Request Account Closure", new Color(146, 43, 33));

        depositBtn.addActionListener(e -> handleDeposit());
        withdrawBtn.addActionListener(e -> handleWithdraw());
        transferBtn.addActionListener(e -> handleTransfer());
        closeBtn.addActionListener(e -> handleRequestClosure());

        actionPanel.add(depositBtn);
        actionPanel.add(withdrawBtn);
        actionPanel.add(transferBtn);
        actionPanel.add(closeBtn);

        // Transaction History Table
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setOpaque(false);

        JLabel tableTitle = new JLabel("Recent Transaction History");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tableTitle.setBorder(new EmptyBorder(5, 0, 5, 0));

        String[] columns = {"Tx ID", "Type", "Amount ($)", "Timestamp", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionTable.setRowHeight(24);
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        transactionTable.getTableHeader().setBackground(new Color(230, 235, 240));

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel middleWrapper = new JPanel(new BorderLayout(10, 10));
        middleWrapper.setOpaque(false);
        middleWrapper.add(actionPanel, BorderLayout.NORTH);
        middleWrapper.add(tablePanel, BorderLayout.CENTER);

        centerPanel.add(middleWrapper, BorderLayout.CENTER);
        return centerPanel;
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(190, 42));
        btn.setFocusPainted(false);
        return btn;
    }

    private void handleDeposit() {
        if (!ensureActiveAccount("deposit")) return;

        String input = JOptionPane.showInputDialog(this, "Enter amount to deposit ($):", "Deposit Funds", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(input.trim());
            if (!Double.isFinite(amount) || amount <= 0) {
                JOptionPane.showMessageDialog(this, "Deposit amount must be a valid positive number.", "Invalid Amount", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = dao.deposit(customerAccount.getAccountNumber(), amount);
            if (success) {
                JOptionPane.showMessageDialog(this, String.format("Successfully deposited $%,.2f into your account!", amount), "Deposit Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAccountState();
            } else {
                JOptionPane.showMessageDialog(this, "Deposit operation failed.", "Operation Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleWithdraw() {
        if (!ensureActiveAccount("withdrawal")) return;

        String input = JOptionPane.showInputDialog(this, "Enter amount to withdraw ($):", "Withdraw Cash", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(input.trim());
            if (!Double.isFinite(amount) || amount <= 0) {
                JOptionPane.showMessageDialog(this, "Withdrawal amount must be a valid positive number.", "Invalid Amount", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (customerAccount.getBalance() < amount) {
                JOptionPane.showMessageDialog(this, String.format("Insufficient balance! Current balance is $%,.2f.", customerAccount.getBalance()), "Insufficient Funds", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = dao.withdraw(customerAccount.getAccountNumber(), amount);
            if (success) {
                JOptionPane.showMessageDialog(this, String.format("Successfully withdrew $%,.2f from your account.", amount), "Withdrawal Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAccountState();
            } else {
                JOptionPane.showMessageDialog(this, "Withdrawal failed. Check balance or account status.", "Operation Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleTransfer() {
        if (!ensureActiveAccount("transfer")) return;

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField targetAccField = new JTextField();
        JTextField amountField = new JTextField();

        panel.add(new JLabel("Target Account Number:"));
        panel.add(targetAccField);
        panel.add(new JLabel("Transfer Amount ($):"));
        panel.add(amountField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Fund Transfer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String targetAccStr = targetAccField.getText().trim();
        String amountStr = amountField.getText().trim();

        if (targetAccStr.isEmpty() || amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Target Account and Amount are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            long targetAcc = Long.parseLong(targetAccStr);
            double amount = Double.parseDouble(amountStr);

            if (targetAcc <= 0) {
                JOptionPane.showMessageDialog(this, "Target Account Number must be a positive number.", "Invalid Target", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (targetAcc == customerAccount.getAccountNumber()) {
                JOptionPane.showMessageDialog(this, "Cannot transfer funds to your own account number.", "Invalid Target", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!Double.isFinite(amount) || amount <= 0) {
                JOptionPane.showMessageDialog(this, "Transfer amount must be a valid positive number.", "Invalid Amount", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (customerAccount.getBalance() < amount) {
                JOptionPane.showMessageDialog(this, String.format("Insufficient balance! Current balance is $%,.2f.", customerAccount.getBalance()), "Insufficient Funds", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = dao.transferFunds(customerAccount.getAccountNumber(), targetAcc, amount);
            if (success) {
                JOptionPane.showMessageDialog(this, String.format("Successfully transferred $%,.2f to Account #%d!", amount, targetAcc), "Transfer Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshAccountState();
            } else {
                JOptionPane.showMessageDialog(this, "Transfer failed! Please check if target account number exists and is active.", "Transfer Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric inputs for Target Account and Amount.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error during transfer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean ensureActiveAccount(String action) {
        loadCustomerAccount();
        if (customerAccount == null) {
            JOptionPane.showMessageDialog(this, "No account found for " + action + ".", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!"Active".equalsIgnoreCase(customerAccount.getStatus())) {
            JOptionPane.showMessageDialog(this, "This account is " + customerAccount.getStatus() + " and cannot be used for " + action + ".", "Account Not Active", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Account closure request
    // ------------------------------------------------------------------

    private void handleRequestClosure() {
        loadCustomerAccount();
        if (customerAccount == null) {
            JOptionPane.showMessageDialog(this, "No account found to close.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!"Active".equalsIgnoreCase(customerAccount.getStatus())) {
            JOptionPane.showMessageDialog(this, "This account is already " + customerAccount.getStatus() + ".", "Account Not Active", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double balance = customerAccount.getBalance();
        if (balance != 0.0) {
            JOptionPane.showMessageDialog(this,
                    String.format("Your account still holds a balance of $%,.2f.%n%nPlease withdraw the full balance before closing your account.", balance),
                    "Cannot Close Account", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextArea reasonArea = new JTextArea(4, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        int reasonResult = JOptionPane.showConfirmDialog(this, new JScrollPane(reasonArea),
                "Please provide a reason for closing your account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (reasonResult != JOptionPane.OK_OPTION) return;

        String reason = reasonArea.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A reason is required to close your account.", "Reason Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Send this account closure request to the bank admin for approval?",
                "Confirm Closure Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = dao.createCustomerRequest(customer.getCustomerId(), customerAccount.getAccountNumber(),
                    "Account Closure", "Account Closure Request", reason);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Your account closure request has been submitted to the bank admin for approval.",
                        "Request Sent", JOptionPane.INFORMATION_MESSAGE);
                loadMyRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Could not submit the closure request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Support & Messages
    // ------------------------------------------------------------------

    private JPanel createSupportPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        panel.setBackground(new Color(245, 247, 250));

        // Feedback form
        JPanel formPanel = new JPanel(new BorderLayout(8, 8));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Send a message to the bank"),
                new EmptyBorder(8, 12, 8, 12)));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topRow.setOpaque(false);
        topRow.add(new JLabel("Type:"));
        requestTypeCombo = new JComboBox<>(new String[]{"Feedback", "Complaint", "Query"});
        requestTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topRow.add(requestTypeCombo);
        topRow.add(new JLabel("Subject:"));
        subjectField = new JTextField(28);
        subjectField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topRow.add(subjectField);

        messageArea = new JTextArea(4, 50);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        JButton sendButton = new JButton("Send Message");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(new Color(40, 116, 166));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> handleSendFeedback());

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> {
            subjectField.setText("");
            messageArea.setText("");
            requestTypeCombo.setSelectedIndex(0);
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonRow.setOpaque(false);
        buttonRow.add(sendButton);
        buttonRow.add(clearButton);

        formPanel.add(topRow, BorderLayout.NORTH);
        formPanel.add(messageScroll, BorderLayout.CENTER);
        formPanel.add(buttonRow, BorderLayout.SOUTH);

        // My requests table
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setOpaque(false);

        JLabel tableTitle = new JLabel("My Requests & Replies");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tableTitle.setBorder(new EmptyBorder(5, 0, 5, 0));

        String[] columns = {"Req ID", "Type", "Subject", "Status", "Admin Response", "Submitted"};
        requestTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestTable = new JTable(requestTableModel);
        requestTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        requestTable.setRowHeight(24);
        requestTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        requestTable.getTableHeader().setBackground(new Color(230, 235, 240));

        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.addActionListener(e -> loadMyRequests());
        JPanel refreshRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshRow.setOpaque(false);
        refreshRow.add(refreshButton);
        tablePanel.add(refreshRow, BorderLayout.SOUTH);

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private void handleSendFeedback() {
        String type = (String) requestTypeCombo.getSelectedItem();
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please write a message before sending.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (subject.isEmpty()) {
            subject = type;
        }
        if (subject.length() > 150) {
            JOptionPane.showMessageDialog(this, "Subject cannot exceed 150 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean ok = dao.createCustomerRequest(customer.getCustomerId(), null, type, subject, message);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Your message has been sent to the bank. You will see the reply here.", "Message Sent", JOptionPane.INFORMATION_MESSAGE);
                subjectField.setText("");
                messageArea.setText("");
                requestTypeCombo.setSelectedIndex(0);
                loadMyRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Could not send your message.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadMyRequests() {
        if (requestTableModel == null) return;
        requestTableModel.setRowCount(0);
        try {
            List<CustomerRequestDTO> list = dao.getCustomerRequests(customer.getCustomerId());
            for (CustomerRequestDTO r : list) {
                Object[] row = {
                        r.requestId(),
                        r.type(),
                        r.subject(),
                        r.status(),
                        r.response() != null ? r.response() : "",
                        r.createdAt()
                };
                requestTableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading requests: " + e.getMessage());
        }
    }

    private void refreshAccountState() {
        loadCustomerAccount();
        if (customerAccount != null) {
            balanceValueLabel.setText(String.format("$%,.2f", customerAccount.getBalance()));
            accountInfoLabel.setText(String.format("Account #: %d  |  Type: %s  |  Status: %s",
                    customerAccount.getAccountNumber(),
                    customerAccount.getAccountType(),
                    customerAccount.getStatus()));
        }
        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        if (customerAccount == null) return;

        try {
            List<Transaction> list = dao.getTransactionHistory(customerAccount.getAccountNumber());
            for (Transaction t : list) {
                Object[] row = {
                        t.getTransactionId(),
                        t.getType(),
                        String.format("%.2f", t.getAmount()),
                        t.getTimestamp(),
                        t.getDescription()
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading transaction history: " + e.getMessage());
        }
    }
}
