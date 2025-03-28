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
     */
    private final RowMapper<ComponentHistory> rowMapper = new RowMapper<ComponentHistory>() {
        @Override
        public ComponentHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            ComponentHistory ch = new ComponentHistory();

            ch.setId(rs.getLong("id"));
            ch.setComponentId(rs.getObject("component_id", UUID.class));
            // Use getObject(...) for parent_id in case it's null
            ch.setParentId(rs.getObject("parent_id", Long.class));

            ch.setStatus(ComponentHistoryStatus.valueOf(rs.getString("status")));
            ch.setUserId(rs.getInt("user_id"));
            ch.setTimestamp(rs.getTimestamp("timestamp"));

            ch.setDdlStatement(rs.getString("ddl_statement"));
            // old_state and new_state are JSONB columns stored as strings here
            ch.setOldState(rs.getString("old_state"));
            ch.setNewState(rs.getString("new_state"));

            return ch;
        }
    };

    /**
     * CREATE: Insert a new record into the `component_history` table.
     * BIGINT `id` is auto-generated, so we do not specify it in the INSERT.
     */
    public int save(ComponentHistory ch) {
        String sql = """
            INSERT INTO component_history
                   (component_id, parent_id, status, user_id, timestamp,
                    ddl_statement, old_state, new_state)
            VALUES (?, ?, ?::component_history_status_enum, ?, ?,
                    ?, ?::jsonb, ?::jsonb)
        """;
        return jdbcTemplate.update(
                sql,
                ch.getComponentId(),
                ch.getParentId(),
                ch.getStatus().name(),
                ch.getUserId(),
                // If timestamp is null, default to "now", or rely on DB default
                ch.getTimestamp() != null ? ch.getTimestamp() : new Timestamp(System.currentTimeMillis()),
                ch.getDdlStatement(),
                // Use "{}" if oldState/newState are null, or store them as-is
                ch.getOldState() != null ? ch.getOldState() : "{}",
                ch.getNewState() != null ? ch.getNewState() : "{}"
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
     * UPDATE: Update fields by ID.
     */
    public int update(ComponentHistory ch) {
        String sql = """
            UPDATE component_history
               SET component_id  = ?,
                   parent_id     = ?,
                   status        = ?::component_history_status_enum,
                   user_id       = ?,
                   timestamp     = ?,
                   ddl_statement = ?,
                   old_state     = ?::jsonb,
                   new_state     = ?::jsonb
             WHERE id = ?
        """;
        return jdbcTemplate.update(
                sql,
                ch.getComponentId(),
                ch.getParentId(),
                ch.getStatus().name(),
                ch.getUserId(),
                ch.getTimestamp(),
                ch.getDdlStatement(),
                ch.getOldState(),
                ch.getNewState(),
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
