package classes;

public record CustomerAccountDTO(
        int customerId,
        int userId,
        String name,
        String email,
        String phone,
        long accountNumber,
        double balance,
        String accountType,
        String status
) {}
