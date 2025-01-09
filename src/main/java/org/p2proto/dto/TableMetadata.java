package org.p2proto.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class to store metadata about a table so we can generate
 * a SQL SELECT statement that includes all columns of this table,
 * plus labels for both singular and plural forms.
 */
public class TableMetadata {

    private final String tableName;           // Physical name from "tables"
    private final String tableLabel;          // Label from nls_labels (label_type='LABEL')
    private final String tablePluralLabel;    // Label from nls_labels (label_type='PLURAL_LABEL')
    private final List<String> columnNames;   // Physical column names from "fields"
    private final Map<String, String> columnLabels; // fieldName -> label (FIELD-level)

    public TableMetadata(String tableName,
                         String tableLabel,
                         String tablePluralLabel,
                         List<String> columnNames,
                         Map<String, String> columnLabels) {
        this.tableName = tableName;
        this.tableLabel = tableLabel;
        this.tablePluralLabel = tablePluralLabel;
        this.columnNames = columnNames;
        this.columnLabels = columnLabels;
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

    public List<String> getColumnNames() {
        return columnNames;
    }

    public Map<String, String> getColumnLabels() {
        return columnLabels;
    }

    /**
     * Generates a SQL SELECT statement for this table, listing all columns.
     */
    public String generateSelectStatement() {
        String cols = columnNames.stream().collect(Collectors.joining(", "));
        return "SELECT " + cols + " FROM " + tableName;
    }
}
