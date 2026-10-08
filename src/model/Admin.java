package model;

public class Admin extends User {
    private int adminId;

    public Admin() {
        super();
        setRole("Admin");
    }

    public Admin(int userId, String username, String password, int adminId) {
        super(userId, username, password, "Admin");
        this.adminId = adminId;
    }

    public Admin(String username, String password, int adminId) {
        super(username, password, "Admin");
        this.adminId = adminId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "adminId=" + adminId +
                ", username='" + getUsername() + '\'' +
                ", userId=" + getUserId() +
                '}';
    }
}
