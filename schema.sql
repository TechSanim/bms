-- To create database and tables

CREATE DATABASE IF NOT EXISTS bank_db;
USE bank_db;

-- Drop tables if they exist to allow clean re-execution
DROP TABLE IF EXISTS customer_requests;
DROP TABLE IF EXISTS admin_activity_log;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

-- 1. Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_online TINYINT(1) NOT NULL DEFAULT 0,
    last_login DATETIME NULL,
    CONSTRAINT chk_user_role CHECK (role IN ('Admin', 'Customer', 'HeadAdmin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Customers Table
CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Accounts Table
CREATE TABLE accounts (
    account_number BIGINT PRIMARY KEY,
    customer_id INT NOT NULL,
    balance DOUBLE NOT NULL DEFAULT 0.0,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    open_date DATE NOT NULL,
    CONSTRAINT chk_account_type CHECK (account_type IN ('Savings', 'Current')),
    CONSTRAINT chk_account_status CHECK (status IN ('Active', 'Closed')),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Transactions Table
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_number BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    CONSTRAINT chk_trans_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Admin Activity Log Table (login / logout audit trail)
CREATE TABLE admin_activity_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Customer Requests Table (account closure requests, feedback, complaints)
CREATE TABLE customer_requests (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


--seed data

-- Seed Users
INSERT INTO users (user_id, username, password, role) VALUES
(1, 'headadmin', 'headadmin123', 'HeadAdmin'),
(2, 'admin', 'admin123', 'Admin'),
(3, 'san', 'user123', 'Customer'),
(4, 'jane_smith', 'user123', 'Customer');

-- Seed Customers
INSERT INTO customers (customer_id, user_id, name, email, phone, address) VALUES
(101, 3, 'San', 'san@example.com', '+1-555-0192', '123 Main Street, New York, NY'),
(102, 4, 'Jane Smith', 'jane.smith@example.com', '+1-555-0148', '456 Oak Avenue, Los Angeles, CA');

-- Seed Accounts
INSERT INTO accounts (account_number, customer_id, balance, account_type, status, open_date) VALUES
(1000000001, 101, 5000.00, 'Savings', 'Active', '2025-01-15'),
(1000000002, 102, 2500.50, 'Current', 'Active', '2025-02-01');

-- Seed Transactions
INSERT INTO transactions (account_number, type, amount, description) VALUES
(1000000001, 'DEPOSIT', 5000.00, 'Initial Account Opening Deposit'),
(1000000002, 'DEPOSIT', 2500.50, 'Initial Account Opening Deposit');
