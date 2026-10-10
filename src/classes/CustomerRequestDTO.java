package classes;

import java.sql.Timestamp;

public record CustomerRequestDTO(
        int requestId,
        int customerId,
        String customerName,
        Long accountNumber,
        String type,
        String subject,
        String message,
        String status,
        String response,
        String handledBy,
        Timestamp createdAt
) {}
