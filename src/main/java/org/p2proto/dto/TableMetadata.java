package org.p2proto.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.p2proto.domain.DomainType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable table metadata with cached lookups and optional decorators.
 * Extends TableSummary to include column information.
 */
@Getter
public class TableMetadata extends TableSummary {

    private final List<ColumnMetaData> columns;               // ordered list for SELECT clause
    private final Map<String, ColumnMetaData> columnsByName;  // cached lookup by exact name
    private final ColumnMetaData primaryKeyMeta;              // PK metadata (non-null)

    @Builder
    private TableMetadata(
            UUID id,
            String tableName,
            String tableLabel,
            String tablePluralLabel,
            TableTypeEnum tableType,
            @Singular("column") List<ColumnMetaData> columns,
            ColumnMetaData primaryKeyMeta
    ) {
        super(id, tableName, tableLabel, tablePluralLabel, tableType);
        this.columns = List.copyOf(Objects.requireNonNullElse(columns, List.of()));

        this.columnsByName = this.columns.stream()
                .collect(Collectors.toUnmodifiableMap(ColumnMetaData::getName, Function.identity(), (a, b) -> a));

        // If pk not explicitly provided, try to infer by looking for a column marked as auto-increment OR named "id"
        this.primaryKeyMeta = primaryKeyMeta;
        if (!this.columnsByName.containsKey(this.primaryKeyMeta.getName())) {
            throw new IllegalArgumentException("primaryKeyMeta column must be present in columns list: " + this.primaryKeyMeta.getName());
        }
    }

    /** SELECT generator using column-provided projections. */
    public String generateSelectStatement() {
        String cols = columns.stream()
                .map(c -> c.generateSelectPart())
                .collect(Collectors.joining(", "));
        return "SELECT " + cols + " FROM " + getTableName();
    }

    /**
     * Build a WHERE clause and ordered args using the provided decorator.
     * The order of args follows iteration order of the input map.
     */
    public Where buildWhere(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new Where("", List.of());
        }
        List<Object> args = new ArrayList<>(conditions.size());
        String where = conditions.entrySet().stream()
                .map(entry -> {
                    ColumnMetaData column = columnsByName.get(entry.getKey());
                    if (column == null) {
                        throw new IllegalArgumentException("Column not found: " + entry.getKey());
                    }
                    args.add(entry.getValue());
                    return column.generateWherePart();
                })
                .collect(Collectors.joining(" AND ", "WHERE ", ""));
        return new Where(where, Collections.unmodifiableList(args));
    }

    /** Simple tuple for WHERE SQL + ordered args. */
    public static final class Where {
        private final String sql;
        private final List<Object> args;

        public Where(String sql, List<Object> args) {
            this.sql = sql;
            this.args = args;
        }
        public String sql() { return sql; }
        public List<Object> args() { return args; }
    }
}
