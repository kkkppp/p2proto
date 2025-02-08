package org.p2proto.util;

import lombok.Getter;
import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.TableMetadata.ColumnMetaData;
import org.p2proto.dto.TableMetadata.DataType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Getter
public class TableMetadataUtil {

    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Query to retrieve table's physical name and logical name.
     */
    private static final String TABLE_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.physical_name " +
                    "FROM tables t WHERE t.id = ?::uuid";

    /**
     * Query to retrieve table labels.
     */
    private static final String TABLE_LABELS_QUERY =
            "SELECT lab.label_text AS table_label, labp.label_text AS table_plural_label " +
                    "FROM nls_labels lab " +
                    "LEFT JOIN nls_labels labp ON labp.component_id = lab.component_id " +
                    "AND labp.language_code = ? AND labp.label_type = 'PLURAL_LABEL' " +
                    "WHERE lab.component_id = ?::uuid AND lab.language_code = ? AND lab.label_type = 'LABEL'";

    /**
     * Query to retrieve field metadata.
     */
    private static final String FIELDS_QUERY =
            "SELECT f.id AS field_id, f.name AS field_name, f.data_type, f.auto_generated " +
                    "FROM fields f WHERE f.table_id = ?::uuid";

    /**
     * Query to retrieve field labels.
     */
    private static final String FIELD_LABELS_QUERY =
            "SELECT nf.component_id AS field_id, nf.label_text AS field_label " +
                    "FROM nls_labels nf WHERE nf.language_code = ? AND nf.label_type = 'LABEL' AND nf.component_id IN (%s)";

    private static final String MAPPING_SQL =
            "SELECT id, logical_name FROM tables";

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {
        // 1) Query table info
        Map<String, Object> tableRow = jdbcTemplate.queryForMap(TABLE_QUERY, tableId);
        if (tableRow == null || tableRow.isEmpty()) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }
        String tableName = (String) tableRow.get("physical_name");
        String logicalName = (String) tableRow.get("logical_name");

        // 2) Query table labels
        Map<String, Object> labelRow = jdbcTemplate.queryForMap(TABLE_LABELS_QUERY, DEFAULT_LANGUAGE, tableId, DEFAULT_LANGUAGE);
        String tableLabel = Optional.ofNullable((String) labelRow.get("table_label")).orElse(logicalName);
        String tablePluralLabel = Optional.ofNullable((String) labelRow.get("table_plural_label")).orElse(tableLabel + "s");

        // 3) Query fields
        List<Map<String, Object>> fieldRows = jdbcTemplate.queryForList(FIELDS_QUERY, tableId);
        List<UUID> fieldIds = fieldRows.stream()
                .map(row -> (UUID) row.get("field_id"))
                .collect(Collectors.toList());

        // 4) Query field labels
        String inClause = fieldIds.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "));
        String fieldLabelsQuery = String.format(FIELD_LABELS_QUERY, inClause);
        Map<UUID, String> fieldLabels = jdbcTemplate.query(fieldLabelsQuery, new Object[]{DEFAULT_LANGUAGE}, rs -> {
            Map<UUID, String> labels = new HashMap<>();
            while (rs.next()) {
                labels.put((UUID) rs.getObject("field_id"), rs.getString("field_label"));
            }
            return labels;
        });

        List<ColumnMetaData> columns = fieldRows.stream().map(row -> {
            String fieldName = (String) row.get("field_name");
            UUID fieldId = (UUID) row.get("field_id");
            String fieldLabel = fieldLabels.getOrDefault(fieldId, fieldName);

            int rawDataType = (int) row.get("data_type");
            TableMetadata.DataType dataType = TableMetadata.DataType.fromCode(rawDataType);

            return new ColumnMetaData(fieldName, fieldLabel, dataType, (Boolean) row.get("auto_generated"), Collections.emptyMap());
        }).collect(Collectors.toList());

        return new TableMetadata(
                tableName,
                tableLabel,
                tablePluralLabel,
                columns
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
