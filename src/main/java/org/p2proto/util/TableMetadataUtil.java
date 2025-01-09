package org.p2proto.util;

import lombok.Getter;
import org.p2proto.dto.TableMetadata;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Getter
public class TableMetadataUtil {

    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Query that retrieves the table's physical_name,
     * plus two label columns:
     *   - lab.label_text (label_type='LABEL')
     *   - labp.label_text (label_type='PLURAL_LABEL')
     */
    private static final String TABLE_QUERY =
            "SELECT t.id AS table_id, " +
                    "       t.logical_name, " +
                    "       t.physical_name, " +
                    "       lab.label_text AS table_label, " +
                    "       labp.label_text AS table_plural_label " +
                    "  FROM tables t " +
                    "  LEFT JOIN nls_labels lab " +
                    "         ON lab.component_id = t.id " +
                    "        AND lab.language_code = '" + DEFAULT_LANGUAGE + "' " +
                    "        AND lab.label_type = 'LABEL' " +
                    "  LEFT JOIN nls_labels labp " +
                    "         ON labp.component_id = t.id " +
                    "        AND labp.language_code = '" + DEFAULT_LANGUAGE + "' " +
                    "        AND labp.label_type = 'PLURAL_LABEL' " +
                    " WHERE t.id = ?::uuid";

    /**
     * Query that retrieves fields + their label (FIELD-level).
     */
    private static final String FIELDS_QUERY =
            "SELECT f.id AS field_id, " +
                    "       f.name AS field_name, " +
                    "       f.data_type, " +
                    "       nf.label_text AS field_label " +
                    "  FROM fields f " +
                    "  LEFT JOIN nls_labels nf " +
                    "         ON nf.component_id = f.id " +
                    "        AND nf.language_code = '" + DEFAULT_LANGUAGE + "' " +
                    "        AND nf.label_type = 'LABEL' " +
                    " WHERE f.table_id = ?::uuid";

    private static final String MAPPING_SQL =
            "SELECT id, logical_name FROM tables";

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {

        // 1) Query table info (including both LABEL and PLURAL_LABEL)
        Map<String, Object> tableRow = jdbcTemplate.queryForMap(TABLE_QUERY, tableId);
        if (tableRow == null || tableRow.isEmpty()) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }

        // Extract tableName, tableLabel, tablePluralLabel
        String tableName = (String) tableRow.get("physical_name");
        if (tableName == null) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }

        String tableLabel = (String) tableRow.get("table_label");
        if (tableLabel == null) {
            // fallback if no row for label_type='LABEL'
            tableLabel = tableName;
        }

        String tablePluralLabel = (String) tableRow.get("table_plural_label");
        if (tablePluralLabel == null) {
            // fallback if no row for label_type='PLURAL_LABEL'
            tablePluralLabel = tableLabel + "s"; // a naive fallback, or just tableName
        }

        // 2) Query fields & build column info
        List<Map<String, Object>> fieldRows = jdbcTemplate.queryForList(FIELDS_QUERY, tableId);

        List<String> columnNames = new ArrayList<>();
        Map<String, String> columnLabels = new HashMap<>();

        for (Map<String, Object> row : fieldRows) {
            String fieldName = (String) row.get("field_name");
            if (fieldName != null) {
                columnNames.add(fieldName);

                String fieldLabel = (String) row.get("field_label");
                if (fieldLabel == null) {
                    fieldLabel = fieldName; // fallback
                }
                columnLabels.put(fieldName, fieldLabel);
            }
        }

        // Build and return a TableMetadata with both singular & plural labels
        return new TableMetadata(
                tableName,
                tableLabel,
                tablePluralLabel,
                columnNames,
                columnLabels
        );
    }

    /**
     * Retrieves a mapping of logical_name -> table UUID for all tables
     */
    @Cacheable(cacheNames = "tables", key = "'logicalNameToId'")
    public Map<String, UUID> findAll() {
        return jdbcTemplate.query(MAPPING_SQL, rs -> {
            Map<String, UUID> map = new HashMap<>();
            while (rs.next()) {
                UUID tableId = (UUID) rs.getObject("id");
                String logicalName = rs.getString("logical_name");
                map.put(logicalName, tableId);
            }
            return map;
        });
    }
}
