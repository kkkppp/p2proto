package org.p2proto.repository.component;

import org.p2proto.model.component.ComponentHistory;
import org.p2proto.model.component.ComponentHistory.ComponentHistoryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ComponentHistoryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * RowMapper to map a row in the `component_history` table to a ComponentHistory object.
     * Make sure the column names match your exact table definition.
     */
    private final RowMapper<ComponentHistory> rowMapper = new RowMapper<ComponentHistory>() {
        @Override
        public ComponentHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            ComponentHistory ch = new ComponentHistory();

            ch.setId(rs.getLong("id"));
            ch.setComponentId(rs.getObject("component_id", UUID.class));

            // Convert the string from the DB to enum
            ch.setStatus(ComponentHistoryStatus.valueOf(rs.getString("status")));

            ch.setUserId(rs.getInt("user_id"));

            Timestamp ts = rs.getTimestamp("timestamp");
            ch.setTimestamp(ts); // or handle null checks if needed

            // JSONB stored as string
            ch.setChangeDetails(rs.getString("change_details"));

            return ch;
        }
    };

    /**
     * CREATE: Insert a new record into the `component_history` table.
     * The BIGINT `id` is auto-generated. We do NOT specify it in the INSERT statement.
     */
    public int save(ComponentHistory ch) {
        String sql = """
            INSERT INTO component_history
                   (component_id, status, user_id, timestamp, change_details)
            VALUES (?, ?::component_history_status_enum, ?, ?, ?::jsonb)
        """;
        return jdbcTemplate.update(
                sql,
                ch.getComponentId(),
                ch.getStatus().name(),
                ch.getUserId(),
                ch.getTimestamp() != null ? ch.getTimestamp() : new Timestamp(System.currentTimeMillis()),
                ch.getChangeDetails() != null ? ch.getChangeDetails() : "{}"
        );
    }

    /**
     * READ ALL: Retrieve all rows from the `component_history` table.
     */
    public List<ComponentHistory> findAll() {
        String sql = "SELECT * FROM component_history";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * READ BY ID: Find a record by its BIGINT (primary key).
     */
    public Optional<ComponentHistory> findById(Long id) {
        String sql = "SELECT * FROM component_history WHERE id = ?";
        List<ComponentHistory> results = jdbcTemplate.query(sql, rowMapper, id);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    /**
     * UPDATE: Update fields by ID (e.g., status, timestamp, changeDetails, etc.)
     */
    public int update(ComponentHistory ch) {
        String sql = """
            UPDATE component_history
               SET status        = ?::component_history_status_enum,
                   user_id       = ?,
                   timestamp     = ?,
                   change_details= ?::jsonb
             WHERE id = ?
        """;
        return jdbcTemplate.update(
                sql,
                ch.getStatus().name(),
                ch.getUserId(),
                ch.getTimestamp(),
                ch.getChangeDetails(),
                ch.getId()
        );
    }

    /**
     * DELETE: Remove a record by its BIGINT ID.
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM component_history WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
