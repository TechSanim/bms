package classes;

public record CustomerAccountDTO(
        int customerId,
        int userId,
        String name,
        String email,
        String phone,
        String address,
        long accountNumber,
        double balance,
        String accountType,
        String status
) {}
