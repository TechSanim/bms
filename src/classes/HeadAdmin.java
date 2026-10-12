package classes;

public class HeadAdmin extends Admin {

    public HeadAdmin() {
        super();
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
