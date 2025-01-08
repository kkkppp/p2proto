package org.p2proto.repository;

import org.p2proto.dto.TableMetadata;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Basic CRUD repository that uses TableMetadata to dynamically build
 * SQL statements for a specific table.
 */
public class TableMetadataCrudRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TableMetadata tableMetadata;
    private final String primaryKeyColumn;

    /**
     * @param jdbcTemplate     Spring's JdbcTemplate for DB operations
     * @param tableMetadata    Metadata for the target table (physical name + columns)
     * @param primaryKeyColumn The name of the primary key column in the table
     */
    public TableMetadataCrudRepository(JdbcTemplate jdbcTemplate,
                                       TableMetadata tableMetadata,
                                       String primaryKeyColumn) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableMetadata = tableMetadata;
        this.primaryKeyColumn = primaryKeyColumn;
    }

    /**
     * Retrieves all rows from the table, returning each row as a Map of column->value
     */
    public List<Map<String, Object>> findAll() {
        String sql = tableMetadata.generateSelectStatement();
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Retrieves a single row by primary key.
     *
     * @param pkValue The primary key value (e.g., UUID or Long, etc.)
     * @return A Map of column->value for the row, or null if none found
     */
    public Map<String, Object> findById(Object pkValue) {
        String sql = tableMetadata.generateSelectStatement()
                + " WHERE " + primaryKeyColumn + " = ?::uuid";
        try {
            return jdbcTemplate.queryForMap(sql, pkValue);
        } catch (EmptyResultDataAccessException e) {
            // No row found
            return null;
        }
    }

    /**
     * Inserts a new row. The row data should map column names to their values.
     * Only columns that exist in TableMetadata will be included in the insert.
     *
     * @param rowData A Map of column->value for the new record
     * @return number of rows affected (should be 1 if successful)
     */
    public int insert(Map<String, Object> rowData) {
        // Filter rowData to only include columns that actually exist in the table
        Map<String, Object> filteredData = rowData.entrySet().stream()
                .filter(e -> tableMetadata.getColumnNames().contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (filteredData.isEmpty()) {
            throw new IllegalArgumentException("No valid columns found in rowData for insert.");
        }

        // Build the column list and placeholders for the INSERT
        List<String> columns = new ArrayList<>(filteredData.keySet());
        String colList = String.join(", ", columns);
        String placeholders = columns.stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableMetadata.getTableName(),
                colList,
                placeholders);

        Object[] values = columns.stream()
                .map(filteredData::get)
                .toArray();

        return jdbcTemplate.update(sql, values);
    }

    /**
     * Updates an existing row identified by the primary key. The row data
     * should contain column->value pairs for what is to be updated.
     *
     * @param pkValue The primary key value (e.g. UUID or Long)
     * @param rowData The columns/values to update
     * @return number of rows affected (should be 1 if successful)
     */
    public int update(Object pkValue, Map<String, Object> rowData) {
        // Filter rowData to only include columns that exist
        Map<String, Object> filteredData = rowData.entrySet().stream()
                .filter(e -> tableMetadata.getColumnNames().contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (filteredData.isEmpty()) {
            throw new IllegalArgumentException("No valid columns found in rowData for update.");
        }

        // Build the "SET col = ?" portion
        List<String> assignments = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filteredData.entrySet()) {
            assignments.add(entry.getKey() + " = ?");
            values.add(entry.getValue());
        }

        // Add the PK value at the end
        values.add(pkValue);

        String setClause = String.join(", ", assignments);
        String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
                tableMetadata.getTableName(),
                setClause,
                primaryKeyColumn);

        return jdbcTemplate.update(sql, values.toArray());
    }

    /**
     * Deletes a row identified by the primary key.
     *
     * @param pkValue The primary key value
     * @return number of rows affected (should be 1 if successful)
     */
    public int delete(Object pkValue) {
        String sql = String.format("DELETE FROM %s WHERE %s = ?",
                tableMetadata.getTableName(),
                primaryKeyColumn);

        return jdbcTemplate.update(sql, pkValue);
    }
}
