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
 */
@Getter
public class TableMetadata {

    private final UUID id;
    private final String tableName;            // Physical name
    private final String tableLabel;           // Singular label
    private final String tablePluralLabel;     // Plural label
    private final TableTypeEnum tableType;

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
        this.id = id;
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.tableLabel = tableLabel;
        this.tablePluralLabel = tablePluralLabel;
        this.tableType = (tableType == null ? TableTypeEnum.STANDARD : tableType);
        this.columns = List.copyOf(Objects.requireNonNullElse(columns, List.of()));

        this.columnsByName = this.columns.stream()
                .collect(Collectors.toUnmodifiableMap(ColumnMetaData::getName, Function.identity(), (a, b) -> a));

        // If pk not explicitly provided, try to infer by looking for a column marked as auto-increment OR named "id"
        this.primaryKeyMeta = Objects.requireNonNullElseGet(
                primaryKeyMeta,
                () -> inferPrimaryKey(this.columns)
        );
        if (!this.columnsByName.containsKey(this.primaryKeyMeta.getName())) {
            throw new IllegalArgumentException("primaryKeyMeta column must be present in columns list: " + this.primaryKeyMeta.getName());
        }
    }

    private static ColumnMetaData inferPrimaryKey(List<ColumnMetaData> cols) {
        return cols.stream()
                .filter(c -> c.getDomain() != null && c.getDomain().isAutoIncrement())
                .findFirst()
                .orElseGet(() ->
                        cols.stream().filter(c -> "id".equalsIgnoreCase(c.getName()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("primaryKeyMeta not provided and cannot be inferred")))
                ;
    }

    /** SELECT generator using column-provided projections. */
    public String generateSelectStatement() {
        return generateSelectStatement(SelectDecorator.defaultDecorator());
    }

    /** SELECT generator with a decorator (e.g., add casts, COALESCE, JSON operators). */
    public String generateSelectStatement(SelectDecorator decorator) {
        String cols = columns.stream()
                .map(c -> decorator.decorate(c.generateSelectPart(), c.getDomain()))
                .collect(Collectors.joining(", "));
        return "SELECT " + cols + " FROM " + tableName;
    }

    /**
     * Build a WHERE clause and ordered args using default decoration.
     * Note: returns empty string and empty list if conditions is empty.
     */
    public Where buildWhere(Map<String, Object> conditions) {
        return buildWhere(conditions, WhereDecorator.defaultDecorator());
    }

    /**
     * Build a WHERE clause and ordered args using the provided decorator.
     * The order of args follows iteration order of the input map.
     */
    public Where buildWhere(Map<String, Object> conditions, WhereDecorator decorator) {
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
                    return decorator.decorate(column.getName(), column.getDomain());
                })
                .collect(Collectors.joining(" AND ", "WHERE ", ""));
        return new Where(where, Collections.unmodifiableList(args));
    }

    /** Exposed PK where fragment (usually "pk = ?" or "pk = ?::uuid"). */
    public String generatePrimaryKeyWhere() {
        return primaryKeyMeta.generateWherePart();
    }

    /** Functional interface for decorating SELECT clause parts. */
    @FunctionalInterface
    public interface SelectDecorator {
        String decorate(String selectPart, DomainType domain);

        static SelectDecorator defaultDecorator() {
            // Keep ColumnMetaData’s own select parts as-is.
            return (selectPart, domain) -> selectPart;
        }
    }

    /** Functional interface for decorating WHERE clause parts. */
    @FunctionalInterface
    public interface WhereDecorator {
        String decorate(String name, DomainType domain);

        static WhereDecorator defaultDecorator() {
            return (name, type) -> type.wherePredicate(name);
        }
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

    public enum TableTypeEnum {
        STANDARD, USERS, ACCESS
    }
}
