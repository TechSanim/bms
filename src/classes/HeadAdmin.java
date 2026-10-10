package classes;

public class HeadAdmin extends Admin {

    public HeadAdmin() {
        super();
        setRole("HeadAdmin");
    }

    public HeadAdmin(int userId, String username, String password) {
        super(userId, username, password, userId);
        setRole("HeadAdmin");
    }

    @Override
    public String toString() {
        return "HeadAdmin{" +
                "userId=" + getUserId() +
                ", username='" + getUsername() + '\'' +
                '}';
    }
}
