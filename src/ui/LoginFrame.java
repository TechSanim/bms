package ui;

import classes.Admin;
import classes.Customer;
import classes.User;
import db.DatabaseOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;
    private JButton exitButton;

    private final DatabaseOperations dao;

    public LoginFrame() {
        this.dao = new DatabaseOperations();
        initUI();
    }

    private void initUI() {
        setTitle("Bank Management System - Secure Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Container Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(26, 82, 118));
        headerPanel.setPreferredSize(new Dimension(450, 80));
        headerPanel.setLayout(new GridBagLayout());

        JLabel headerLabel = new JLabel("BANK MANAGEMENT SYSTEM");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Role Selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(roleLabel, gbc);

        gbc.gridx = 1;
        String[] roles = {"Customer", "Admin", "HeadAdmin"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(roleComboBox, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(passwordField, gbc);

        // Button Panel
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(40, 116, 166));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new Dimension(110, 35));

        exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        exitButton.setPreferredSize(new Dimension(90, 35));

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        formPanel.add(buttonPanel, gbc);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Event Listeners
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        exitButton.addActionListener(e -> System.exit(0));

        // Press Enter to submit login
        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleComboBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Username and Password.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = dao.validateLogin(username, password, role);

            if (user != null) {
                JOptionPane.showMessageDialog(this, "Login Successful! Welcome, " + user.getUsername(), "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();

                if (user instanceof Admin) {
                    new AdminDashboard((Admin) user).setVisible(true);
                } else if (user instanceof Customer) {
                    new CustomerDashboard((Customer) user).setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials or role mismatch. Please try again.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database Connection Error:\n" + ex.getMessage() + "\n\nPlease ensure MySQL is running and the credentials in DBConfig are correct.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
