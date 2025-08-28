package org.p2proto.repository.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
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

    /** Query to retrieve table's physical name and logical name. */
    public static final String TABLE_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.type " +
                    "FROM tables t WHERE t.id = ?::uuid";

    /** Query to retrieve single table labels (from components.nls_labels). */
    public static final String SINGLE_TABLE_LABELS_QUERY =
            "SELECT " +
                    "  c.nls_labels #>> ARRAY[?,'LABEL']        AS table_label, " +
                    "  c.nls_labels #>> ARRAY[?,'PLURAL_LABEL'] AS table_plural_label " +
                    "FROM components c " +
                    "WHERE c.id = ?::uuid";

    /** Query to retrieve all tables with their labels for a language (single pass). */
    public static final String ALL_TABLES_WITH_LABELS_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.type, " +
                    "       c.nls_labels #>> ARRAY[?,'LABEL']        AS label, " +
                    "       c.nls_labels #>> ARRAY[?,'PLURAL_LABEL'] AS plural_label " +
                    "  FROM tables t " +
                    "  JOIN components c ON c.id = t.id";

    /** Query to retrieve field metadata + field label from components.nls_labels. */
    public static final String FIELDS_QUERY =
            "SELECT f.id AS field_id, f.name AS field_name, f.data_type, f.auto_generated, " +
                    "       f.removable, f.default_value, f.properties, " +
                    "       (c.nls_labels #>> ARRAY[?,'LABEL']) AS label_text " +
                    "  FROM fields f " +
                    "  JOIN components c ON c.id = f.id " +
                    " WHERE f.table_id = ?::uuid";

    public static final String ALL_TABLES_QUERY =
            "SELECT id, logical_name, type FROM tables";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Retrieves a mapping of logical_name -> table UUID for all tables */
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
     * Loads table-level info (including LABEL & PLURAL_LABEL for DEFAULT_LANGUAGE)
     * and returns them in ascending order by tablePluralLabel.
     */
    @Cacheable(cacheNames = "tables", key = "'allTablesOrderedByPluralLabel'")
    public List<TableMetadata> findAllWithLabels() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                ALL_TABLES_WITH_LABELS_QUERY, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE
        );
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<TableMetadata> results = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            UUID tableId = (UUID) row.get("table_id");
            String logicalName = (String) row.get("logical_name");
            TableMetadata.TableTypeEnum tableType =
                    TableMetadata.TableTypeEnum.valueOf(row.get("type").toString());

            String label = (String) row.get("label");
            String plural = (String) row.get("plural_label");

            String tableLabel = label != null ? label : logicalName;
            String tablePluralLabel = plural != null ? plural : tableLabel + "s";

            results.add(new TableMetadata(
                    tableId,
                    logicalName,
                    tableLabel,
                    tablePluralLabel,
                    tableType,
                    Collections.emptyList() // no columns here
            ));
        }

        results.sort(Comparator.comparing(TableMetadata::getTablePluralLabel));
        return results;
    }

    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {
        // 1) table info
        Map<String, Object> tableRow = jdbcTemplate.queryForMap(TABLE_QUERY, tableId);
        if (tableRow.isEmpty()) {
            throw new RuntimeException("No table found with ID = " + tableId);
        }
        String logicalName = (String) tableRow.get("logical_name");
        TableMetadata.TableTypeEnum tableType =
                TableMetadata.TableTypeEnum.valueOf(tableRow.get("type").toString());

        // 2) table labels from JSONB
        Map<String, Object> labelRow = jdbcTemplate.queryForMap(
                SINGLE_TABLE_LABELS_QUERY, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE, tableId
        );
        String tableLabel = Optional.ofNullable((String) labelRow.get("table_label"))
                .orElse(logicalName);
        String tablePluralLabel = Optional.ofNullable((String) labelRow.get("table_plural_label"))
                .orElse(tableLabel + "s");

        // 3) fields with labels from JSONB
        List<Map<String, Object>> fieldRows =
                jdbcTemplate.queryForList(FIELDS_QUERY, DEFAULT_LANGUAGE, tableId);

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
            return new ColumnMetaData(
                    fieldId, fieldName,
                    fieldLabel, // label from components.nls_labels
                    domain,
                    (Boolean) row.get("primary_key"),
                    (Boolean) row.get("auto_generated"),
                    (Boolean) row.get("removable"),
                    defaultValue,
                    Collections.emptyMap()
            );
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

    /**
     * Inserts the table row (assumes the corresponding components row already exists with the same id),
     * and sets labels in components.nls_labels for DEFAULT_LANGUAGE.
     */
    public void createMetadataInDb(TableMetadata table) {
        String tableSql =
                "INSERT INTO tables (id, TYPE, logical_name, removable) " +
                        "VALUES (?, ?::table_type_enum, ?, ?)";

        jdbcTemplate.update(tableSql,
                table.getId(),
                table.getTableType().name(),
                table.getTableName(),
                true
        );

        // Merge LABEL and PLURAL_LABEL into JSONB (deep-safe)
        String upsertLabelsSql =
                "UPDATE components SET nls_labels = " +
                        "  jsonb_set( " +
                        "    jsonb_set( COALESCE(nls_labels,'{}'::jsonb), ARRAY[?,'LABEL'],        to_jsonb(?::text), true), " +
                        "    ARRAY[?,'PLURAL_LABEL'], to_jsonb(?::text), true" +
                        "  ) " +
                        "WHERE id = ?::uuid";

        jdbcTemplate.update(upsertLabelsSql,
                DEFAULT_LANGUAGE, table.getTableLabel(),
                DEFAULT_LANGUAGE, table.getTablePluralLabel(),
                table.getId()
        );

        for (ColumnMetaData column : table.getColumns()) {
            createColumnMetadataInDb(column, table.getId());
        }
    }

    /**
     * Updates labels in components.nls_labels for a table (DEFAULT_LANGUAGE).
     */
    public void updateMetadataInDb(TableMetadata table) {
        String upsertLabelsSql =
                "UPDATE components SET nls_labels = " +
                        "  jsonb_set( " +
                        "    jsonb_set( COALESCE(nls_labels,'{}'::jsonb), ARRAY[?,'LABEL'],        to_jsonb(?::text), true), " +
                        "    ARRAY[?,'PLURAL_LABEL'], to_jsonb(?::text), true" +
                        "  ) " +
                        "WHERE id = ?::uuid";

        jdbcTemplate.update(upsertLabelsSql,
                DEFAULT_LANGUAGE, table.getTableLabel(),
                DEFAULT_LANGUAGE, table.getTablePluralLabel(),
                table.getId()
        );
    }

    /**
     * Inserts the field row (assumes components row for the field id exists),
     * and sets the field LABEL in components.nls_labels for DEFAULT_LANGUAGE.
     */
    public void createColumnMetadataInDb(ColumnMetaData column, UUID tableId) {
        String sql =
                "INSERT INTO fields " +
                        " (id, table_id, name, data_type, removable, primary_key, auto_generated, default_value, properties) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)";

        String defaultValueJson;
        try {
            defaultValueJson = column.getDefaultValue() == null
                    ? null
                    : column.getDefaultValue().toJson();
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
                column.getAdditionalProperties() != null
                        ? column.getAdditionalProperties().toString()
                        : "{}"
        );

        // Set/merge the field's LABEL into components.nls_labels
        String upsertLabelSql =
                "UPDATE components SET nls_labels = " +
                        "  jsonb_set( COALESCE(nls_labels,'{}'::jsonb), ARRAY[?,'LABEL'], to_jsonb(?::text), true) " +
                        "WHERE id = ?::uuid";

        jdbcTemplate.update(upsertLabelSql,
                DEFAULT_LANGUAGE, column.getLabel(), column.getId()
        );
    }
}