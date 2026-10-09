package ui;

import classes.Account;
import classes.Customer;
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

    // GUI Components
    private JLabel balanceValueLabel;
    private JLabel accountInfoLabel;
    private JTable transactionTable;
    private DefaultTableModel tableModel;

    public CustomerDashboard(Customer customer) {
        this.customer = customer;
        this.dao = new DatabaseOperations();
        loadCustomerAccount();
        initUI();
        loadTransactionHistory();
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
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(21, 67, 96));
        headerPanel.setPreferredSize(new Dimension(900, 65));
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

        // Center Container
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        centerPanel.setBackground(new Color(245, 247, 250));

        // 1. Account Summary Banner Card
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

        // 2. Action Controls Panel (Deposit, Withdraw, Transfer)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        actionPanel.setOpaque(false);

        JButton depositBtn = createActionButton("Deposit Funds", new Color(39, 174, 96));
        JButton withdrawBtn = createActionButton("Withdraw Cash", new Color(211, 84, 0));
        JButton transferBtn = createActionButton("Transfer Funds", new Color(41, 128, 185));

        depositBtn.addActionListener(e -> handleDeposit());
        withdrawBtn.addActionListener(e -> handleWithdraw());
        transferBtn.addActionListener(e -> handleTransfer());

        actionPanel.add(depositBtn);
        actionPanel.add(withdrawBtn);
        actionPanel.add(transferBtn);

        // 3. Transaction History Table
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

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(170, 42));
        btn.setFocusPainted(false);
        return btn;
    }

    private void handleDeposit() {
        if (customerAccount == null) {
            JOptionPane.showMessageDialog(this, "No active account found for deposit.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
        if (customerAccount == null) {
            JOptionPane.showMessageDialog(this, "No active account found for withdrawal.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
        if (customerAccount == null) {
            JOptionPane.showMessageDialog(this, "No active account found for transfer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
