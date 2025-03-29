package org.p2proto.util;

import lombok.Getter;
import org.p2proto.dto.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Getter
public class UserUtil {
    private final JdbcTemplate jdbcTemplate;

    public UserUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CurrentUser findUserByUuid(UUID userUuid) {
        final String sql = """
            SELECT id, uuid, username, first_name, last_name, email
            FROM users
            WHERE uuid = ?
        """;

        // If you want to handle the case where no row is found, you can
        // catch EmptyResultDataAccessException and return null or throw
        // a custom exception instead.
        return jdbcTemplate.queryForObject(
                sql,
                new Object[] { userUuid },
                (rs, rowNum) -> {
                    Integer id = rs.getInt("id");
                    UUID uuid = rs.getObject("uuid", UUID.class);
                    String username = rs.getString("username");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String email = rs.getString("email");

                    // Construct a "fullName" from first + last.
                    String fullName = (firstName == null ? "" : firstName)
                            + " "
                            + (lastName == null ? "" : lastName);

                    return new CurrentUser(id, uuid, username, fullName.trim(), email);
                }
        );
    }
}
