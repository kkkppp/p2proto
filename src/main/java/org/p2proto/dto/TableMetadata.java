package org.p2proto.dto;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class TableMetadata {

    private final String tableName;         // The physical name of the table
    private final List<String> columnNames; // Columns in the table

    public TableMetadata(String tableName, List<String> columnNames) {
        this.tableName = tableName;
        this.columnNames = columnNames;
    }

    public String generateSelectStatement() {
        String allColumns = columnNames.stream().collect(Collectors.joining(", "));
        return "SELECT " + allColumns + " FROM " + tableName;
    }

}
