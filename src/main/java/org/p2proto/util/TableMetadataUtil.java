package org.p2proto.util;

import lombok.Getter;
import org.p2proto.ddl.Domain;
import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.TableMetadata.ColumnMetaData;
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
            "SELECT t.id AS table_id, t.logical_name " +
                    "FROM tables t WHERE t.id = ?::uuid";

    /**
     * Query to retrieve single table labels.
     */
    private static final String SINGLE_TABLE_LABELS_QUERY =
            "SELECT lab.label_text AS table_label, labp.label_text AS table_plural_label " +
                    "FROM nls_labels lab " +
                    "LEFT JOIN nls_labels labp ON labp.component_id = lab.component_id " +
                    "AND labp.language_code = ? AND labp.label_type = 'PLURAL_LABEL' " +
                    "WHERE lab.component_id = ?::uuid AND lab.language_code = ? AND lab.label_type = 'LABEL'";

    /**
     * Query to retrieve all table's labels.
     */
    private static final String ALL_TABLES_LABELS_QUERY =
            "SELECT nl.component_id AS table_id, nl.label_text, nl.label_type " +
                    "  FROM nls_labels nl " +
                    " WHERE nl.language_code = '%s' " +
                    "   AND nl.component_id IN (SELECT t.id FROM tables t) " +
                    "   AND nl.label_type IN ('LABEL', 'PLURAL_LABEL')";
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

    private static final String ALL_TABLES_QUERY =
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
        String logicalName = (String) tableRow.get("logical_name");

        // 2) Query table labels
        Map<String, Object> labelRow = jdbcTemplate.queryForMap(SINGLE_TABLE_LABELS_QUERY, DEFAULT_LANGUAGE, tableId, DEFAULT_LANGUAGE);
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
            Domain domain = Domain.fromCode(rawDataType);

            return new ColumnMetaData(fieldName, fieldLabel, domain, (Boolean) row.get("auto_generated"), Collections.emptyMap());
        }).collect(Collectors.toList());

        return new TableMetadata(
                tableId,
                logicalName,
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
        return jdbcTemplate.query(ALL_TABLES_QUERY, rs -> {
            Map<String, UUID> map = new HashMap<>();
            while (rs.next()) {
                UUID tableId = (UUID) rs.getObject("id");
                String logicalName = rs.getString("logical_name");
                map.put(logicalName, tableId);
            }
            return map;
        });
    }

    /**
     * Loads table-level info and returns them
     * in ascending order by tablePluralLabel.
     */
    @Cacheable(cacheNames = "tables", key = "'allTablesOrderedByPluralLabel'")
    public List<TableMetadata> findAllWithLabels() {
        // 1) Load all tables
        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(ALL_TABLES_QUERY);
        if (tableRows.isEmpty()) {
            return Collections.emptyList();
        }

        // Map tableId -> TableMetadata
        Map<UUID, TableMetadata> tableMap = new LinkedHashMap<>();
        for (Map<String, Object> row : tableRows) {
            UUID tableId = (UUID) row.get("id");
            String logicalName = (String) row.get("logical_name");

            // We won't load columns, so set columns = Collections.emptyList()
            tableMap.put(tableId, new TableMetadata(
                    tableId,
                    logicalName,
                    null, // tableLabel
                    null, // tablePluralLabel
                    Collections.emptyList()
            ));
        }

        // 2) Load labels (LABEL & PLURAL_LABEL) with no JOIN
        String labelsQuery = String.format(ALL_TABLES_LABELS_QUERY, DEFAULT_LANGUAGE);
        Map<UUID, Map<String, String>> tableLabelsMap = new HashMap<>();

        jdbcTemplate.query(labelsQuery, rs -> {
                UUID tableId = (UUID) rs.getObject("table_id");
                String labelText = rs.getString("label_text");
                String labelType = rs.getString("label_type");
                tableLabelsMap
                        .computeIfAbsent(tableId, k -> new HashMap<>())
                        .put(labelType, labelText);
        });

        // 3) Apply labels
        for (Map.Entry<UUID, TableMetadata> entry : tableMap.entrySet()) {
            UUID tableId = entry.getKey();
            TableMetadata meta = entry.getValue();

            String fallback = meta.getTableName();
            Map<String, String> typeMap = tableLabelsMap.get(tableId);

            if (typeMap != null) {
                String singular = typeMap.getOrDefault("LABEL", fallback);
                String plural = typeMap.getOrDefault("PLURAL_LABEL", singular + "s");
                meta.setTableLabel(singular);
                meta.setTablePluralLabel(plural);
            } else {
                meta.setTableLabel(fallback);
                meta.setTablePluralLabel(fallback + "s");
            }
        }

        // 4) Sort by plural label (ascending)
        List<TableMetadata> results = new ArrayList<>(tableMap.values());
        results.sort(Comparator.comparing(TableMetadata::getTablePluralLabel));

        // 5) Return sorted list
        return results;
    }
}
