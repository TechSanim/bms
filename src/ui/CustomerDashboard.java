package ui;

import model.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CustomerDashboard extends JFrame {

    private final Customer customer;

    public CustomerDashboard(Customer customer) {
        this.customer = customer;
        initUI();
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

        // Center: intentionally left blank for now
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(245, 247, 250));
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        add(mainPanel);
    }
}
