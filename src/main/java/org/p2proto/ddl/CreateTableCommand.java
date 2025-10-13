package org.p2proto.ddl;

import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.statement.DatabaseFunction;
import lombok.Data;
import org.p2proto.dto.ColumnDefaultHolder;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;
import org.p2proto.service.TableService;

import java.util.List;
import java.util.Objects;

@Data
public class CreateTableCommand implements DDLCommand {

    private final TableMetadata table;

    /**
     * Convenience ctor for a fresh table using default columns and STANDARD type.
     * Primary key is inferred from default columns (by primaryKey flag, auto-increment, or "id").
     */
    public CreateTableCommand(String tableName, String tableLabel, String tableLabelPlural) {
        List<ColumnMetaData> cols = TableService.defaultColumns();
        ColumnMetaData pk = cols.stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrimaryKey()))
                .findFirst()
                .orElse(null); // let TableMetadata infer if not explicitly marked

        this.table = TableMetadata.builder()
                .id(null)
                .tableName(Objects.requireNonNull(tableName, "tableName"))
                .tableLabel(tableLabel == null ? "" : tableLabel)
                .tablePluralLabel(tableLabelPlural == null ? "" : tableLabelPlural)
                .tableType(TableMetadata.TableTypeEnum.STANDARD)
                .columns(cols)
                .primaryKeyMeta(pk) // may be null; TableMetadata will infer
                .build();
    }

    /**
     * Use when metadata is already constructed (immutable TableMetadata).
     */
    public CreateTableCommand(TableMetadata table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    @Override
    public Change getChange() {
        CreateTableChange result = new CreateTableChange();
        result.setTableName(table.getTableName());

        for (ColumnMetaData column : table.getColumns()) {
            AddColumnConfig columnConfig = new AddColumnConfig();
            columnConfig.setName(column.getName());
            columnConfig.setType(column.getDomain().getLiquibaseType());
            columnConfig.setAutoIncrement(column.getDomain().isAutoIncrement());

            if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                ConstraintsConfig pk = new ConstraintsConfig();
                pk.setPrimaryKey(Boolean.TRUE);
                columnConfig.setConstraints(pk);
            }

            ColumnDefaultHolder def = column.getDefaultValue();
            if (def != null
                    && def.getExecutionContext() == ColumnDefaultHolder.ExecutionContext.SERVER_SIDE
                    && def.getTriggerEvent() == ColumnDefaultHolder.TriggerEvent.ON_CREATE) {
                // e.g., CURRENT_TIMESTAMP or other DB function
                columnConfig.setDefaultValueComputed(new DatabaseFunction(def.getValue()));
            }

            result.addColumn(columnConfig);
        }

        return result;
    }
}
