package classes;

import java.sql.Timestamp;

public record AdminDTO(
        int userId,
        String username,
        boolean online,
        Timestamp lastLogin
) {}
