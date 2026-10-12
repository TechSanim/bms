package classes;

import java.util.Date;

public class Account {
    private long accountNumber;
    private int customerId;
    private double balance;
    private String accountType; // "Savings", "Current"
    private String status;      // "Active", "Closed"
    private Date openDate;

    public Account() {
    }

    public Account(long accountNumber, int customerId, double balance, String accountType) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = balance;
        this.accountType = accountType;
        this.status = "Active";
        this.openDate = new Date();
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNumber=" + accountNumber +
                ", customerId=" + customerId +
                ", balance=" + balance +
                ", accountType='" + accountType + '\'' +
                ", status='" + status + '\'' +
                ", openDate=" + openDate +
                '}';
    }
}
