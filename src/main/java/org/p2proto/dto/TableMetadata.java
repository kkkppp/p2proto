package org.p2proto.dto;

import org.p2proto.model.record.FormField;
import org.p2proto.model.record.FieldType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class to store metadata about a table to generate
 * SQL SELECT statements, including labels for singular and plural forms.
 */
public class TableMetadata {

    private final String tableName;           // Physical name from "tables"
    private final String tableLabel;          // Label from nls_labels (label_type='LABEL')
    private final String tablePluralLabel;    // Label from nls_labels (label_type='PLURAL_LABEL')
    private final List<ColumnMetaData> columns;   // Column information as a list of ColumnMetaData objects

    public TableMetadata(String tableName,
                         String tableLabel,
                         String tablePluralLabel,
                         List<ColumnMetaData> columns) {
        this.tableName = tableName;
        this.tableLabel = tableLabel;
        this.tablePluralLabel = tablePluralLabel;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    /** Singular label (e.g., "User") **/
    public String getTableLabel() {
        return tableLabel;
    }

    /** Plural label (e.g., "Users") **/
    public String getTablePluralLabel() {
        return tablePluralLabel;
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
    public static class ColumnMetaData {
        private final String name;        // Column name
        private final String label;       // Column label
        private final DataType dataType;  // Data type
        private final Map<String, String> additionalProperties; // Additional properties
        private final SelectDecorator selectDecorator; // Decorator for generating SELECT parts
        private final WhereDecorator whereDecorator;   // Decorator for generating WHERE parts

        public ColumnMetaData(String name, String label, DataType dataType, Map<String, String> additionalProperties) {
            this(name, label, dataType, additionalProperties, SelectDecorator.defaultDecorator(), WhereDecorator.defaultDecorator());
        }

        public ColumnMetaData(String name, String label, DataType dataType, Map<String, String> additionalProperties, SelectDecorator selectDecorator, WhereDecorator whereDecorator) {
            this.name = name;
            this.label = label;
            this.dataType = dataType;
            this.additionalProperties = additionalProperties;
            this.selectDecorator = selectDecorator;
            this.whereDecorator = whereDecorator;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public DataType getDataType() {
            return dataType;
        }

        public Map<String, String> getAdditionalProperties() {
            return additionalProperties;
        }

        /**
         * Utility method to generate FormField representation.
         */
        public FormField toFormField() {
            FormField formField = new FormField();
            formField.setName(name);
            formField.setLabel(label);
            formField.setType(mapDataTypeToFieldType(dataType));
            formField.setRequired(false); // Default value, can be customized based on additional properties
            return formField;
        }

        private FieldType mapDataTypeToFieldType(DataType dataType) {
            switch (dataType) {
                case TEXT:
                    return FieldType.TEXT;
                case DATE:
                    return FieldType.DATE;
                case DATETIME:
                    return FieldType.DATETIME;
                case UUID:
                case INTEGER:
                case AUTOINCREMENT:
                    return FieldType.NUMBER;
                case BOOLEAN:
                    return FieldType.CHECKBOX;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
        }

        /**
         * Generates the part of the SELECT clause for this column.
         */
        public String generateSelectPart() {
            return selectDecorator.decorate(name, dataType);
        }

        /**
         * Generates the part of the WHERE clause for this column using a placeholder.
         *
         * @return The WHERE clause part with a placeholder.
         */
        public String generateWherePart() {
            return whereDecorator.decorate(name, dataType);
        }
    }

    /**
     * Functional interface for decorating SELECT clause parts.
     */
    @FunctionalInterface
    public interface SelectDecorator {
        String decorate(String name, DataType dataType);

        static SelectDecorator defaultDecorator() {
            return (name, dataType) -> name;
        }
    }

    /**
     * Functional interface for decorating WHERE clause parts.
     */
    @FunctionalInterface
    public interface WhereDecorator {
        String decorate(String name, DataType dataType);

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

    /**
     * Enum to define possible data types for columns.
     */
    public enum DataType {
        INTEGER(1),
        TEXT(2),
        UUID(3),
        DATE(4),
        DATETIME(5),
        BOOLEAN(6),
        FLOAT(7),
        AUTOINCREMENT(8);

        private final int code;

        DataType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static DataType fromCode(int code) {
            for (DataType type : values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown DataType code: " + code);
        }
    }

}
