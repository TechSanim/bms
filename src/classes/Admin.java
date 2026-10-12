package classes;

public class Admin extends User {
    private int adminId;

    public Admin() {
        super();
        setRole("Admin");
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
