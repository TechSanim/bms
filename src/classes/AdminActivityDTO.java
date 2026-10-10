package classes;

import java.sql.Timestamp;

public record AdminActivityDTO(
        int logId,
        String username,
        String action,
        Timestamp timestamp
) {}
