package org.p2proto.repository.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.p2proto.ddl.Domain;
import org.p2proto.dto.ColumnDefaultHolder;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TableRepository {
    public static final String DEFAULT_LANGUAGE = "en";
    /**
     * Query to retrieve table's physical name and logical name.
     */
    public static final String TABLE_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.type " +
                    "FROM tables t WHERE t.id = ?::uuid";
    /**
     * Query to retrieve single table labels.
     */
    public static final String SINGLE_TABLE_LABELS_QUERY =
            "SELECT lab.label_text AS table_label, labp.label_text AS table_plural_label " +
                    "FROM nls_labels lab " +
                    "LEFT JOIN nls_labels labp ON labp.component_id = lab.component_id " +
                    "AND labp.language_code = ? AND labp.label_type = 'PLURAL_LABEL' " +
                    "WHERE lab.component_id = ?::uuid AND lab.language_code = ? AND lab.label_type = 'LABEL'";
    /**
     * Query to retrieve all table's labels.
     */
    public static final String ALL_TABLES_LABELS_QUERY =
            "SELECT nl.component_id AS table_id, nl.label_text, nl.label_type " +
                    "  FROM nls_labels nl " +
                    " WHERE nl.language_code = '%s' " +
                    "   AND nl.component_id IN (SELECT t.id FROM tables t) " +
                    "   AND nl.label_type IN ('LABEL', 'PLURAL_LABEL')";
    /**
     * Query to retrieve field metadata.
     */
    public static final String FIELDS_QUERY =
            "SELECT f.id AS field_id, f.name AS field_name, f.data_type, f.auto_generated, f.removable, f.default_value, f.properties, l.label_text" +
                    " FROM fields f, nls_labels l WHERE f.table_id = ?::uuid and f.id = l.component_id and l.label_type='LABEL' and l.language_code= ?";

    public static final String ALL_TABLES_QUERY =
                            "SELECT id, logical_name, type FROM tables";

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(TableRepository.ALL_TABLES_QUERY);
        if (tableRows.isEmpty()) {
            return Collections.emptyList();
        }

        // Map tableId -> TableMetadata
        Map<UUID, TableMetadata> tableMap = new LinkedHashMap<>();
        for (Map<String, Object> row : tableRows) {
            UUID tableId = (UUID) row.get("id");
            String logicalName = (String) row.get("logical_name");
            TableMetadata.TableTypeEnum tableType = TableMetadata.TableTypeEnum.valueOf(row.get("type").toString());

            // We won't load columns, so set columns = Collections.emptyList()
            tableMap.put(tableId, new TableMetadata(
                    tableId,
                    logicalName,
                    null, // tableLabel
                    null, // tablePluralLabel
                    tableType,
                    Collections.emptyList()
            ));
        }

        // 2) Load labels (LABEL & PLURAL_LABEL) with no JOIN
        String labelsQuery = String.format(TableRepository.ALL_TABLES_LABELS_QUERY, TableRepository.DEFAULT_LANGUAGE);
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

    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {
        // 1) Query table info
        Map<String, Object> tableRow = jdbcTemplate.queryForMap(TableRepository.TABLE_QUERY, tableId);
        if (tableRow.isEmpty()) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }
        String logicalName = (String) tableRow.get("logical_name");
        TableMetadata.TableTypeEnum tableType = TableMetadata.TableTypeEnum.valueOf(tableRow.get("type").toString());
        // 2) Query table labels
        Map<String, Object> labelRow = jdbcTemplate.queryForMap(TableRepository.SINGLE_TABLE_LABELS_QUERY, TableRepository.DEFAULT_LANGUAGE, tableId, TableRepository.DEFAULT_LANGUAGE);
        String tableLabel = Optional.ofNullable((String) labelRow.get("table_label")).orElse(logicalName);
        String tablePluralLabel = Optional.ofNullable((String) labelRow.get("table_plural_label")).orElse(tableLabel + "s");

        // 3) Query fields
        List<Map<String, Object>> fieldRows = jdbcTemplate.queryForList(TableRepository.FIELDS_QUERY, tableId, TableRepository.DEFAULT_LANGUAGE);

        List<ColumnMetaData> columns = fieldRows.stream().map(row -> {
            String fieldName = (String) row.get("field_name");
            UUID fieldId = (UUID) row.get("field_id");
            String fieldLabel = (String) row.get("label_text");

            int rawDataType = (int) row.get("data_type");
            Domain domain = Domain.fromCode(rawDataType);

            ColumnDefaultHolder defaultValue;
            try {
                PGobject tmp = (PGobject) row.get("default_value");
                defaultValue = tmp == null ? null : ColumnDefaultHolder.fromJson(tmp.getValue());
            } catch (JsonProcessingException e) {
                log.error(e.getLocalizedMessage());
                defaultValue = null;
            }
            return new ColumnMetaData(fieldId, fieldName, fieldLabel, domain, (Boolean) row.get("primary_key"),
                    (Boolean) row.get("auto_generated"), (Boolean) row.get("removable"), defaultValue,
                    Collections.emptyMap());
        }).collect(Collectors.toList());

        return new TableMetadata(
                tableId,
                logicalName,
                tableLabel,
                tablePluralLabel,
                tableType,
                columns
        );
    }

    public void createMetadataInDb(TableMetadata table) {
        String tableSql = "INSERT INTO tables (id, TYPE, logical_name, removable) VALUES (?, ?::table_type_enum, ?, ?)";

        jdbcTemplate.update(tableSql,
                table.getId(),
                table.getTableType().name(),
                table.getTableName(),
                true
        );

        String labelSql = "INSERT INTO nls_labels (component_id, language_code, label_type, label_text) VALUES (?, ?, ?::label_type_enum, ?)";

        jdbcTemplate.update(labelSql, table.getId(), DEFAULT_LANGUAGE, "LABEL", table.getTableLabel());
        jdbcTemplate.update(labelSql, table.getId(), DEFAULT_LANGUAGE, "PLURAL_LABEL", table.getTablePluralLabel());

        for (ColumnMetaData column : table.getColumns()) {
            createColumnMetadataInDb(column, table.getId());
        }
    }

    public void updateMetadataInDb(TableMetadata table) {
/*        String tableSql = "INSERT INTO tables (id, TYPE, logical_name, removable) VALUES (?, ?::table_type_enum, ?, ?)";

        jdbcTemplate.update(tableSql,
                table.getId(),
                table.getTableType().name(),
                table.getTableName(),
                true
        );*/

        String labelSql = "update nls_labels set label_text = ? where component_id= ?::uuid and label_type=?::label_type_enum and language_code=?";

        jdbcTemplate.update(labelSql, table.getTableLabel(), table.getId(), "LABEL", DEFAULT_LANGUAGE);
        jdbcTemplate.update(labelSql, table.getTablePluralLabel(), table.getId(), "PLURAL_LABEL", DEFAULT_LANGUAGE);

    }
        public void createColumnMetadataInDb(ColumnMetaData column, UUID tableId) {
        String sql = "INSERT INTO fields (id, table_id, name, data_type, removable, primary_key, auto_generated, default_value, properties) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)";

            String defaultValueJson;
            try {
                defaultValueJson = column.getDefaultValue() == null ? null : column.getDefaultValue().toJson();
            } catch (JsonProcessingException e) {
                log.error(e.getLocalizedMessage());
                defaultValueJson = "{}";
            }
            jdbcTemplate.update(sql,
                column.getId(),
                tableId,
                column.getName(),
                column.getDomain().getCode(),
                column.getRemovable(),
                column.getPrimaryKey(),
                column.getAutoGenerated(),
                defaultValueJson,
                column.getAdditionalProperties() != null ? column.getAdditionalProperties().toString() : "{}"

        );

        String labelSql = "INSERT INTO nls_labels (component_id, language_code, label_type, label_text) VALUES (?, ?, ?::label_type_enum, ?)";

        jdbcTemplate.update(labelSql, column.getId(), DEFAULT_LANGUAGE, "LABEL", column.getLabel());

    }
}
