package org.p2proto.ddl;

import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.statement.DatabaseFunction;
import lombok.Data;
import org.p2proto.dto.ColumnDefaultHolder;
import org.p2proto.dto.TableMetadata;
import org.p2proto.service.TableService;

@Data
public class CreateTableCommand implements DDLCommand {

    private final TableMetadata table;

    public CreateTableCommand(String tableName, String tableLabel, String tableLabelPlural) {
        table = new TableMetadata(tableName, tableLabel, tableLabelPlural);
        table.setTableType(TableMetadata.TableTypeEnum.STANDARD);
        table.setColumns(TableService.defaultColumns());
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
            ColumnDefaultHolder defaultValue = column.getDefaultValue();
            if (defaultValue != null && defaultValue.getExecutionContext().equals(ColumnDefaultHolder.ExecutionContext.SERVER_SIDE) && defaultValue.getTriggerEvent().equals(ColumnDefaultHolder.TriggerEvent.ON_CREATE)) {
                columnConfig.setDefaultValueComputed(new DatabaseFunction(defaultValue.getValue()));
            }
            result.addColumn(columnConfig);
        });
        return result;
    }
}
