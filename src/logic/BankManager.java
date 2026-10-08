package logic;

import model.Account;
import model.Admin;
import model.Customer;
import model.CustomerAccountDTO;
import model.User;

import java.util.ArrayList;
import java.util.List;

public final class BankManager {

    private static final BankManager INSTANCE = new BankManager();

    private final Admin demoAdmin = new Admin(1, "admin", "admin123", 1);
    private final List<Customer> customers = new ArrayList<>();
    private final List<Account> accounts = new ArrayList<>();
    private int nextId = 2;

    private BankManager() {
        Customer demo = new Customer(2, "user", "user123", 1, "John Doe",
                "john@example.com", "555-0100", "123 Main Street");
        customers.add(demo);
        accounts.add(new Account(1000000001L, 1, 1000.00, "Savings"));
        nextId = 3;
    }

    public static BankManager getInstance() {
        return INSTANCE;
    }

    public synchronized User validateLogin(String username, String password, String role) {
        if ("Admin".equals(role)) {
            if (demoAdmin.getUsername().equals(username) && demoAdmin.getPassword().equals(password)) {
                return demoAdmin;
            }
        } else if ("Customer".equals(role)) {
            for (Customer c : customers) {
                if (c.getUsername().equals(username) && c.getPassword().equals(password)) {
                    return c;
                }
            }
        }
        return null;
    }

    public synchronized boolean usernameExists(String username) {
        if (demoAdmin.getUsername().equals(username)) {
            return true;
        }
        for (Customer c : customers) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean accountNumberExists(long accountNumber) {
        for (Account a : accounts) {
            if (a.getAccountNumber() == accountNumber) {
                return true;
            }
        }
        return false;
    }

    public synchronized Customer registerCustomer(String username, String password, String name,
                                                  String email, String phone, String address,
                                                  long accountNumber, double initialDeposit,
                                                  String accountType) {
        if (usernameExists(username) || accountNumberExists(accountNumber)) {
            return null;
        }
        int id = nextId++;
        Customer customer = new Customer(id, username, password, id, name, email, phone, address);
        Account account = new Account(accountNumber, id, initialDeposit, accountType);
        customers.add(customer);
        accounts.add(account);
        return customer;
    }

    public synchronized List<CustomerAccountDTO> getAllCustomersWithAccounts() {
        List<CustomerAccountDTO> list = new ArrayList<>();
        for (Customer c : customers) {
            Account match = null;
            for (Account a : accounts) {
                if (a.getCustomerId() == c.getCustomerId()) {
                    match = a;
                    break;
                }
            }
            if (match != null) {
                list.add(new CustomerAccountDTO(
                        c.getCustomerId(), c.getUserId(), c.getName(), c.getEmail(),
                        c.getPhone(), c.getAddress(), match.getAccountNumber(),
                        match.getBalance(), match.getAccountType(), match.getStatus()));
            }
        }
        return list;
    }
}
