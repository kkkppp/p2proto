package org.p2proto.dto;

import lombok.Data;
import org.p2proto.ddl.Domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A class to store metadata about a table to generate
 * SQL SELECT statements, including labels for singular and plural forms.
 */
@Data
public class TableMetadata {

    private UUID id;
    private String tableName;           // Physical name from "tables"
    private String tableLabel;          // Label from nls_labels (label_type='LABEL')
    private String tablePluralLabel;    // Label from nls_labels (label_type='PLURAL_LABEL')
    private TableTypeEnum tableType;
    private List<ColumnMetaData> columns;   // Column information as a list of ColumnMetaData objects

    public TableMetadata() {
    }

    public TableMetadata(String tableName, String tableLabel, String tablePluralLabel) {
        this(null, tableName, tableLabel, tablePluralLabel, TableTypeEnum.STANDARD, List.of());
    }

    public TableMetadata(UUID id,
                         String tableName,
                         String tableLabel,
                         String tablePluralLabel,
                         TableTypeEnum tableType,
                         List<ColumnMetaData> columns) {
        this.id = id;
        this.tableName = tableName;
        this.tableLabel = tableLabel;
        this.tablePluralLabel = tablePluralLabel;
        this.tableType = tableType;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnMetaData> getColumns() {
        return columns;
    }

    /**
     * Generates a SQL SELECT statement for this table, listing all columns.
     */
    public String generateSelectStatement() {
        String cols = columns.stream()
                .map(org.p2proto.dto.ColumnMetaData::generateSelectPart)
                .collect(Collectors.joining(", "));
        return "SELECT " + cols + " FROM " + tableName;
    }

    /**
     * Generates a WHERE clause for the given conditions.
     *
     * @param conditions A map of column names to their filter values.
     * @return A SQL WHERE clause.
     */
    public String generateWhereClause(Map<String, Object> conditions) {
        return conditions.entrySet().stream()
                .map(entry -> {
                    ColumnMetaData column = columns.stream()
                            .filter(c -> c.getName().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Column " + entry.getKey() + " not found."));
                    return column.generateWherePart();
                })
                .collect(Collectors.joining(" AND ", "WHERE ", ""));
    }

    /**
     * Functional interface for decorating SELECT clause parts.
     */
    @FunctionalInterface
    public interface SelectDecorator {
        String decorate(String name, Domain domain);

        static SelectDecorator defaultDecorator() {
            return (name, dataType) -> name;
        }
    }

    /**
     * Functional interface for decorating WHERE clause parts.
     */
    @FunctionalInterface
    public interface WhereDecorator {
        String decorate(String name, Domain domain);

        static WhereDecorator defaultDecorator() {
            return (name, dataType) -> {
                switch (dataType) {
                    case TEXT:
                    case DATE:
                    case DATETIME:
                        return name + " = ?";
                    case UUID:
                        return name + " = ?::uuid";
                    case INTEGER:
                    case FLOAT:
                    case BOOLEAN:
                    case AUTOINCREMENT:
                        return name + " = ?";
                    default:
                        throw new UnsupportedOperationException("Unsupported data type: " + dataType);
                }
            };
        }
    }

    public enum TableTypeEnum {
        STANDARD,
        USERS,
        ACCESS;
    }
}
