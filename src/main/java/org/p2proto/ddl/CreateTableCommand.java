package org.p2proto.ddl;

import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import lombok.Data;
import org.p2proto.dto.TableMetadata;
import org.p2proto.service.TableMetadataUtil;

@Data
public class CreateTableCommand implements DDLCommand {

    private final TableMetadata table;

    public CreateTableCommand(String tableName, String tableLabel, String tableLabelPlural) {
        table = new TableMetadata(tableName, tableLabel, tableLabelPlural);
        table.setColumns(TableMetadataUtil.defaultColumns());
    }

    public CreateTableCommand(TableMetadata table) {
        this.table = table;
    }

    @Override
    public Change getChange() {
        CreateTableChange result = new CreateTableChange();
        result.setTableName(table.getTableName());
        table.getColumns().forEach(column -> {
            AddColumnConfig columnConfig = new AddColumnConfig();
            columnConfig.setName(column.getName());
            columnConfig.setType(column.getDomain().getLiquibaseType());
            columnConfig.setAutoIncrement(column.getDomain().isAutoIncrement());
            if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                ConstraintsConfig pk = new ConstraintsConfig();
                pk.setPrimaryKey(Boolean.TRUE);
                columnConfig.setConstraints(pk);
            }
            result.addColumn(columnConfig);
        });
        return result;
    }
}
