package org.p2proto.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.p2proto.ddl.Domain;
import org.p2proto.model.record.FieldType;
import org.p2proto.model.record.FormField;

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
    private List<ColumnMetaData> columns;   // Column information as a list of ColumnMetaData objects

    public TableMetadata() {
    }

    public TableMetadata(String tableName, String tableLabel, String tablePluralLabel) {
        this(null, tableName, tableLabel, tablePluralLabel, List.of());
    }

    public TableMetadata(UUID id,
                         String tableName,
                         String tableLabel,
                         String tablePluralLabel,
                         List<ColumnMetaData> columns) {
        this.id = id;
        this.tableName = tableName;
        this.tableLabel = tableLabel;
        this.tablePluralLabel = tablePluralLabel;
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
                .map(ColumnMetaData::generateSelectPart)
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
     * Inner class to hold column information.
     */
    @Data
    public static class ColumnMetaData {
        private final String name;        // Column name
        private final String label;       // Column label
        private final org.p2proto.ddl.Domain domain;  // Data type
        private final Boolean primaryKey;
        private final Boolean autoGenerated;
        private final Map<String, String> additionalProperties; // Additional properties
        private final SelectDecorator selectDecorator; // Decorator for generating SELECT parts
        private final WhereDecorator whereDecorator;   // Decorator for generating WHERE parts

        public ColumnMetaData(String name, String label, org.p2proto.ddl.Domain dataType, Map<String, String> additionalProperties) {
            this(name, label, dataType, false, false, additionalProperties, SelectDecorator.defaultDecorator(), WhereDecorator.defaultDecorator());
        }

        public ColumnMetaData(String name, String label, org.p2proto.ddl.Domain dataType, Boolean primaryKey, Boolean autoGenerated, Map<String, String> additionalProperties) {
            this(name, label, dataType, primaryKey, autoGenerated, additionalProperties, SelectDecorator.defaultDecorator(), WhereDecorator.defaultDecorator());
        }

        public ColumnMetaData(String name, String label, org.p2proto.ddl.Domain dataType, Boolean primaryKey, Boolean autoGenerated, Map<String, String> additionalProperties, SelectDecorator selectDecorator, WhereDecorator whereDecorator) {
            this.name = name;
            this.label = label;
            this.domain = dataType;
            this.primaryKey = primaryKey;
            this.autoGenerated = autoGenerated;
            this.additionalProperties = additionalProperties;
            this.selectDecorator = selectDecorator;
            this.whereDecorator = whereDecorator;
        }

        /**
         * Utility method to generate FormField representation.
         */
        public FormField toFormField() {
            FormField formField = new FormField();
            formField.setName(name);
            formField.setLabel(label);
            formField.setType(mapDataTypeToFieldType(domain));
            formField.setAutoGenerated(autoGenerated);
            formField.setRequired(false); // Default value, can be customized based on additional properties
            return formField;
        }

        private FieldType mapDataTypeToFieldType(org.p2proto.ddl.Domain domain) {
            return switch (domain) {
                case TEXT -> FieldType.TEXT;
                case DATE -> FieldType.DATE;
                case DATETIME -> FieldType.DATETIME;
                case UUID, INTEGER, AUTOINCREMENT -> FieldType.NUMBER;
                case BOOLEAN -> FieldType.CHECKBOX;
                case PASSWORD -> FieldType.PASSWORD;
                default -> throw new IllegalArgumentException("Unsupported data type: " + domain);
            };
        }

        /**
         * Generates the part of the SELECT clause for this column.
         */
        public String generateSelectPart() {
            return selectDecorator.decorate(name, domain);
        }

        /**
         * Generates the part of the WHERE clause for this column using a placeholder.
         *
         * @return The WHERE clause part with a placeholder.
         */
        public String generateWherePart() {
            return whereDecorator.decorate(name, domain);
        }
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

}
