package org.p2proto.repository.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper OM = new ObjectMapper();

    /** Table core info. */
    public static final String TABLE_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.type " +
                    "  FROM tables t " +
                    " WHERE t.id = ?::uuid";

    /** Single table labels (from components.nls_labels). */
    public static final String SINGLE_TABLE_LABELS_QUERY =
            "SELECT " +
                    "  c.nls_labels #>> ARRAY[?,'LABEL']        AS table_label, " +
                    "  c.nls_labels #>> ARRAY[?,'PLURAL_LABEL'] AS table_plural_label " +
                    "  FROM components c " +
                    " WHERE c.id = ?::uuid";

    /** All tables with labels in one pass. */
    public static final String ALL_TABLES_WITH_LABELS_QUERY =
            "SELECT t.id AS table_id, t.logical_name, t.type, " +
                    "       c.nls_labels #>> ARRAY[?,'LABEL']        AS label, " +
                    "       c.nls_labels #>> ARRAY[?,'PLURAL_LABEL'] AS plural_label " +
                    "  FROM tables t " +
                    "  JOIN components c ON c.id = t.id";

    /** Fields for a table (include primary_key + auto_generated!). */
    public static final String FIELDS_QUERY =
            "SELECT f.id AS field_id, f.name AS field_name, f.data_type, " +
                    "       f.primary_key, f.removable, f.auto_generated, " +
                    "       f.default_value, f.properties, " +
                    "       (c.nls_labels #>> ARRAY[?,'LABEL']) AS label_text " +
                    "  FROM fields f " +
                    "  JOIN components c ON c.id = f.id " +
                    " WHERE f.table_id = ?::uuid";

    public static final String ALL_TABLES_QUERY =
            "SELECT id, logical_name, type FROM tables";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** logical_name -> table UUID */
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
     * Loads table list with labels (DEFAULT_LANGUAGE), sorted by plural label.
     */
    @Cacheable(cacheNames = "tables", key = "'allTablesOrderedByPluralLabel'")
    public List<TableMetadata> findAllWithLabels() {
        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(ALL_TABLES_WITH_LABELS_QUERY, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE);

        if (rows.isEmpty()) return List.of();

        List<TableMetadata> results = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            UUID tableId = (UUID) row.get("table_id");
            String logicalName = (String) row.get("logical_name");
            TableMetadata.TableTypeEnum tableType =
                    TableMetadata.TableTypeEnum.valueOf(String.valueOf(row.get("type")));

            String label = (String) row.get("label");
            String plural = (String) row.get("plural_label");

            String tableLabel = (label != null) ? label : logicalName;
            String tablePluralLabel = (plural != null) ? plural : tableLabel + "s";

            results.add(TableMetadata.builder()
                    .id(tableId)
                    .tableName(logicalName)
                    .tableLabel(tableLabel)
                    .tablePluralLabel(tablePluralLabel)
                    .tableType(tableType)
                    .columns(List.of())          // not loading fields here
                    .primaryKeyMeta(null)        // not known in this lightweight call
                    .build());
        }

        results.sort(Comparator.comparing(TableMetadata::getTablePluralLabel));
        return results;
    }

    @Cacheable(cacheNames = "tables", key = "#tableId")
    public TableMetadata findByID(UUID tableId) {
        // 1) table info
        Map<String, Object> tableRow = jdbcTemplate.queryForMap(TABLE_QUERY, tableId);
        String logicalName = (String) tableRow.get("logical_name");
        TableMetadata.TableTypeEnum tableType =
                TableMetadata.TableTypeEnum.valueOf(String.valueOf(tableRow.get("type")));

        // 2) labels
        Map<String, Object> labelRow =
                jdbcTemplate.queryForMap(SINGLE_TABLE_LABELS_QUERY, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE, tableId);
        String tableLabel = Optional.ofNullable((String) labelRow.get("table_label")).orElse(logicalName);
        String tablePluralLabel = Optional.ofNullable((String) labelRow.get("table_plural_label"))
                .orElse(tableLabel + "s");

        // 3) fields
        List<Map<String, Object>> fieldRows =
                jdbcTemplate.queryForList(FIELDS_QUERY, DEFAULT_LANGUAGE, tableId);

        List<ColumnMetaData> columns = fieldRows.stream().map(row -> {
            UUID fieldId = (UUID) row.get("field_id");
            String fieldName = (String) row.get("field_name");
            String fieldLabel = (String) row.get("label_text");

            int rawDataType = (int) row.get("data_type");
            Domain domain = Domain.fromCode(rawDataType);

            Boolean primaryKey = getBoolean(row.get("primary_key"));
            Boolean removable  = getBoolean(row.get("removable"));
            // auto_generated exists, but your Domain already encodes this;
            // if you need it elsewhere, read: Boolean autoGen = getBoolean(row.get("auto_generated"));

            ColumnDefaultHolder defaultValue = null;
            try {
                PGobject pg = (PGobject) row.get("default_value");
                if (pg != null && pg.getValue() != null) {
                    defaultValue = ColumnDefaultHolder.fromJson(pg.getValue());
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse default_value for field {}: {}", fieldName, e.getMessage());
            }

            Map<String, String> props = parseProperties(row.get("properties"));

            // Use the constructor that sets default decorators:
            return new ColumnMetaData(
                    fieldId,
                    fieldName,
                    (fieldLabel != null ? fieldLabel : fieldName),
                    domain,
                    (primaryKey != null ? primaryKey : Boolean.FALSE),
                    (removable  != null ? removable  : Boolean.FALSE),
                    defaultValue,
                    props
            );
        }).collect(Collectors.toUnmodifiableList());

        // Determine PK meta from the loaded columns
        ColumnMetaData pkMeta = columns.stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrimaryKey()))
                .findFirst()
                .orElse(null); // TableMetadata can still infer if null

        return TableMetadata.builder()
                .id(tableId)
                .tableName(logicalName)
                .tableLabel(tableLabel)
                .tablePluralLabel(tablePluralLabel)
                .tableType(tableType)
                .columns(columns)
                .primaryKeyMeta(pkMeta)
                .build();
    }

    /**
     * Insert table row (components row is assumed to already exist with the same id),
     * and set labels (DEFAULT_LANGUAGE).
     */
    public void createMetadataInDb(TableMetadata table) {
        String tableSql =
                "INSERT INTO tables (id, type, logical_name, removable) " +
                        "VALUES (?, ?::table_type_enum, ?, ?)";

        jdbcTemplate.update(tableSql,
                table.getId(),
                table.getTableType().name(),
                table.getTableName(),
                true
        );

        upsertTableLabels(table);

        for (ColumnMetaData column : table.getColumns()) {
            createColumnMetadataInDb(column, table.getId());
        }
    }

    /** Update table labels only (DEFAULT_LANGUAGE). */
    public void updateMetadataInDb(TableMetadata table) {
        upsertTableLabels(table);
    }

    private void upsertTableLabels(TableMetadata table) {
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
     * Insert field row and set field LABEL (DEFAULT_LANGUAGE).
     */
    public void createColumnMetadataInDb(ColumnMetaData column, UUID tableId) {
        String sql =
                "INSERT INTO fields " +
                        " (id, table_id, name, data_type, removable, primary_key, auto_generated, default_value, properties) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)";

        String defaultValueJson = toJsonOrNull(column.getDefaultValue());
        String propsJson        = toJsonOrEmptyObject(column.getAdditionalProperties());

        jdbcTemplate.update(sql,
                column.getId(),
                tableId,
                column.getName(),
                column.getDomain().getCode(),
                safeBool(column.getRemovable()),
                safeBool(column.getPrimaryKey()),
                column.getDomain().isAutoIncrement(),  // auto_generated
                defaultValueJson,
                propsJson
        );

        // Upsert field label
        String upsertLabelSql =
                "UPDATE components SET nls_labels = " +
                        "  jsonb_set( COALESCE(nls_labels,'{}'::jsonb), ARRAY[?,'LABEL'], to_jsonb(?::text), true) " +
                        "WHERE id = ?::uuid";

        jdbcTemplate.update(upsertLabelSql,
                DEFAULT_LANGUAGE, column.getLabel(), column.getId()
        );
    }

    // ---- helpers ------------------------------------------------------------

    private static Boolean getBoolean(Object o) {
        return (o == null) ? null : (o instanceof Boolean b ? b : Boolean.valueOf(String.valueOf(o)));
    }

    private static boolean safeBool(Boolean b) {
        return b != null && b;
    }

    private static Map<String, String> parseProperties(Object dbVal) {
        if (dbVal == null) return Collections.emptyMap();
        try {
            if (dbVal instanceof PGobject pg && pg.getValue() != null) {
                // JSONB string
                @SuppressWarnings("unchecked")
                Map<String, String> map = OM.readValue(pg.getValue(), Map.class);
                return map == null ? Collections.emptyMap() : map;
            }
            if (dbVal instanceof String s && !s.isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = OM.readValue(s, Map.class);
                return map == null ? Collections.emptyMap() : map;
            }
        } catch (Exception e) {
            log.error("Failed to parse properties json: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private static String toJsonOrNull(ColumnDefaultHolder def) {
        if (def == null) return null;
        try {
            return def.toJson();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize default_value: {}", e.getMessage());
            return null; // store NULL if not serializable
        }
    }

    private static String toJsonOrEmptyObject(Map<String, String> map) {
        try {
            return OM.writeValueAsString(map == null ? Map.of() : map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize properties: {}", e.getMessage());
            return "{}";
        }
    }
}
