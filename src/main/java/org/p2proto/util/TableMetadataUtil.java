package org.p2proto.util;

import org.p2proto.dto.TableMetadata;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class (as a Spring service) to create a TableMetadata instance for a single table
 * by reading from "tables" (physical_name) and "fields" (column names),
 * then letting Spring's caching abstraction handle Infinispan under the hood.
 */
@Service
public class TableMetadataUtil {

    private static final String TABLE_SQL = "SELECT physical_name FROM tables WHERE id = ?::uuid";
    private static final String FIELDS_SQL = "SELECT name FROM fields WHERE table_id = ?::uuid";
    private static final String MAPPING_SQL = "SELECT id, logical_name FROM tables";


    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves (and automatically caches) the TableMetadata for a given tableId.
     *
     * @param tableId UUID primary key in the "tables" table
     * @return a TableMetadata object containing the table’s physical name and its columns
     */
    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {
        // 1) Retrieve the physical_name from the 'tables' table
        String tableName = jdbcTemplate.queryForObject(
                TABLE_SQL,
                String.class,
                tableId
        );

        if (tableName == null) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }

        // 2) Retrieve the column names from 'fields'
        List<String> columnNames = jdbcTemplate.queryForList(
                FIELDS_SQL,
                String.class,
                tableId
        );

        // 3) Build and return the TableMetadata (will be cached automatically by Spring)
        return new TableMetadata(tableName, columnNames);
    }

    /**
     * Retrieves a mapping of logical_name -> table UUID for *all* tables.
     *
     * Annotate with @Cacheable if you want Spring to cache the result
     * in the same or another cache. Here we use "tableMappings" cache with a static key.
     */
    @Cacheable(cacheNames = "tables", key = "'logicalNameToId'")
    public Map<String, UUID> findAll() {
        // Query the entire "tables" table for (id, logical_name)
        return jdbcTemplate.query(MAPPING_SQL, rs -> {
            Map<String, UUID> map = new HashMap<>();
            while (rs.next()) {
                // Convert the "id" column to a UUID
                UUID tableId = (UUID) rs.getObject("id");
                String logicalName = rs.getString("logical_name");
                map.put(logicalName, tableId);
            }
            return map;
        });
    }
}
