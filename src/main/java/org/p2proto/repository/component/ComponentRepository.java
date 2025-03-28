package org.p2proto.repository.component;

import org.p2proto.model.component.Component;
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
public class ComponentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * RowMapper to map a row in the `components` table to a Component object.
     * Make sure the column names match your exact table definition.
     */
    private final RowMapper<Component> rowMapper = new RowMapper<Component>() {
        @Override
        public Component mapRow(ResultSet rs, int rowNum) throws SQLException {
            Component component = new Component();

            component.setId(rs.getObject("id", UUID.class));

            component.setComponentType(Component.ComponentTypeEnum.valueOf(rs.getString("component_type")));
            component.setStatus(Component.ComponentStatusEnum.valueOf(rs.getString("status")));

            component.setCreatedAt(rs.getTimestamp("created_at"));
            component.setUpdatedAt(rs.getTimestamp("updated_at"));
            component.setCreatedBy(rs.getObject("created_by", Integer.class));
            component.setUpdatedBy(rs.getObject("updated_by", Integer.class));

            return component;
        }
    };

    /**
     * CREATE: Insert a new record.
     * The UUID `id` is auto-generated by default (uuid_generate_v4()),
     * so we typically do NOT specify it in the INSERT statement.
     */
    public UUID save(Component component) {
        // Postgres-specific syntax: "RETURNING id" returns the generated UUID
        String sql = """
        INSERT INTO components (component_type, status, created_by)
        VALUES (?, ?, ?)
        RETURNING id
    """;

        // queryForObject(...) will run the INSERT, then return the 'id' as a UUID
        UUID newId = jdbcTemplate.queryForObject(
                sql,
                new Object[] {
                        component.getComponentType().name(),
                        component.getStatus().name(),
                        component.getCreatedBy()
                },
                UUID.class
        );

        // Optionally set the new UUID into the Component object
        component.setId(newId);

        // Return it if you want to use it elsewhere (e.g., confirm insertion)
        return newId;
    }

    /**
     * READ ALL: Retrieve all rows from the `components` table.
     */
    public List<Component> findAll() {
        String sql = "SELECT * FROM components";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * READ BY ID: Find a component by its UUID (primary key).
     */
    public Optional<Component> findById(UUID id) {
        String sql = "SELECT * FROM components WHERE id = ?";
        List<Component> results = jdbcTemplate.query(sql, rowMapper, id);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    /**
     * UPDATE: Modify certain fields by ID.
     * Typically, updated_at is set to now(), but you can also pass it in yourself.
     */
    public int update(Component component) {
        String sql = """
            UPDATE components
               SET status         = ?, 
                   updated_at     = ?, 
                   updated_by     = ?
             WHERE id = ?
        """;

        return jdbcTemplate.update(
                sql,
                component.getStatus().name(),
                component.getUpdatedAt(),
                component.getUpdatedBy(),
                component.getId()
        );
    }

    /**
     * DELETE: Remove a component by its UUID.
     */
    public int deleteById(UUID id) {
        String sql = "DELETE FROM components WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
