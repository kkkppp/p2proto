package org.p2proto.service;

import liquibase.exception.DatabaseException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.p2proto.ddl.CreateTableCommand;
import org.p2proto.ddl.DDLExecutor;
import org.p2proto.domain.DomainFactory;
import org.p2proto.domain.DomainType;
import org.p2proto.dto.ColumnDefaultHolder;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.CurrentUser;
import org.p2proto.dto.TableMetadata;
import org.p2proto.model.component.Component;
import org.p2proto.model.component.ComponentHistory;
import org.p2proto.repository.table.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Getter
@Slf4j
public class TableService {

    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    private final JdbcTemplate jdbcTemplate;
    private final DDLExecutor ddlExecutor;
    private final ComponentService componentService;
    private final TableRepository tableRepository;

    @Autowired
    public TableService(ComponentService componentService,
                        JdbcTemplate jdbcTemplate,
                        TableRepository tableRepository,
                        DDLExecutor ddlExecutor) {
        this.componentService = componentService;
        this.jdbcTemplate = jdbcTemplate;
        this.ddlExecutor = ddlExecutor;
        this.tableRepository = tableRepository;
    }

    // ---------- Public API ----------

    @Transactional(propagation = Propagation.SUPPORTS)
    public void createTable(String tableName, String tableLabel, String tablePluralLabel, CurrentUser currentUser) {
        createTableInternal(new CreateTableCommand(tableName, tableLabel, tablePluralLabel), currentUser);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void createTable(TableMetadata tableMetadata, CurrentUser currentUser) {
        createTableInternal(new CreateTableCommand(tableMetadata), currentUser);
    }

    // ---------- Internals ----------

    private void createTableInternal(CreateTableCommand command, CurrentUser currentUser) {
        Component tableComponent = null;
        Long historyId = null;

        try {
            TableMetadata inputMeta = command.getTable();

            // 1) Create TABLE component + history BEFORE DDL
            tableComponent = componentService.createComponent(
                    Component.ComponentTypeEnum.TABLE,
                    Component.ComponentStatusEnum.LOCKED,
                    currentUser.getCurrentUserId()
            );
            historyId = componentService.createHistory(
                    tableComponent.getId(),
                    ComponentHistory.ComponentHistoryStatus.IN_PROGRESS,
                    currentUser.getCurrentUserId()
            );

            // 2) Execute DDL
            List<String> ddl = ddlExecutor.executeDDL(command);

            // 3) Create FIELD components AFTER successful DDL
            Map<String, UUID> colIds = new LinkedHashMap<>();
            for (ColumnMetaData c : inputMeta.getColumns()) {
                Component fieldComponent = componentService.createComponent(
                        Component.ComponentTypeEnum.FIELD,
                        Component.ComponentStatusEnum.ACTIVE,
                        currentUser.getCurrentUserId()
                );
                componentService.createHistory(
                        fieldComponent.getId(),
                        ComponentHistory.ComponentHistoryStatus.COMPLETED,
                        currentUser.getCurrentUserId()
                );
                colIds.put(c.getName(), fieldComponent.getId());
            }

            // 4) Rebuild columns with assigned component IDs (preserve decorators)
            List<ColumnMetaData> rebuiltCols = inputMeta.getColumns().stream()
                    .map(c -> new ColumnMetaData(
                            colIds.get(c.getName()),            // id (new)
                            c.getName(),
                            c.getLabel(),
                            c.getDomain(),
                            c.getPrimaryKey(),
                            c.getRemovable(),
                            c.getDefaultValue(),
                            c.getAdditionalProperties()
                    ))
                    .collect(Collectors.toUnmodifiableList());

            // Repoint PK meta to the rebuilt instance (by name), if present
            String pkName = inputMeta.getPrimaryKeyMeta() != null ? inputMeta.getPrimaryKeyMeta().getName() : null;
            ColumnMetaData rebuiltPk = (pkName == null) ? null :
                    rebuiltCols.stream().filter(c -> c.getName().equals(pkName)).findFirst().orElse(null);

            // 5) Build final immutable TableMetadata with table component ID + rebuilt columns
            TableMetadata metaToPersist = TableMetadata.builder()
                    .id(tableComponent.getId())
                    .tableName(inputMeta.getTableName())
                    .tableLabel(inputMeta.getTableLabel())
                    .tablePluralLabel(inputMeta.getTablePluralLabel())
                    .tableType(inputMeta.getTableType())
                    .columns(rebuiltCols)
                    .primaryKeyMeta(rebuiltPk) // allow inference if null
                    .build();

            // 6) Persist metadata and mark success
            tableRepository.createMetadataInDb(metaToPersist);
            componentService.markSuccess(tableComponent.getId(), historyId, ddl);

        } catch (DatabaseException | SQLException e) {
            log.error("Failed to create table", e);
            if (tableComponent != null) {
                componentService.markFailure(tableComponent.getId(), historyId);
            }
        }
    }

    // ---------- Defaults ----------

    /**
     * Default columns:
     *  - id          AUTOINCREMENT (PK)
     *  - summary     TEXT
     *  - created_at  DATETIME (client-side default CURRENT_TIMESTAMP on create)
     *  - updated_at  DATETIME (client-side default CURRENT_TIMESTAMP on update)
     */
    public static List<ColumnMetaData> defaultColumns() {
        ColumnMetaData idCol = new ColumnMetaData(
                null,                       // id (component id assigned later)
                "id",
                "ID",
                DomainFactory.fromInternalName("AUTOINCREMENT"),
                true,                       // primaryKey
                false,                      // removable
                null,                       // default
                Map.of()                    // additional props
        );

        ColumnDefaultHolder createdAtDef = ColumnDefaultHolder.builder()
                .executionContext(ColumnDefaultHolder.ExecutionContext.CLIENT_SIDE)
                .triggerEvent(ColumnDefaultHolder.TriggerEvent.ON_CREATE)
                .valueType(ColumnDefaultHolder.DefaultValueType.FORMULA)
                .value(CURRENT_TIMESTAMP)
                .build();

        ColumnMetaData createdAtCol = new ColumnMetaData(
                "created_at",
                "Created At",
                DomainFactory.fromInternalName("DATETIME"),
                createdAtDef,
                Map.of()
        );

        ColumnDefaultHolder updatedAtDef = ColumnDefaultHolder.builder()
                .executionContext(ColumnDefaultHolder.ExecutionContext.CLIENT_SIDE)
                .triggerEvent(ColumnDefaultHolder.TriggerEvent.ON_UPDATE)
                .valueType(ColumnDefaultHolder.DefaultValueType.FORMULA)
                .value(CURRENT_TIMESTAMP)
                .build();

        ColumnMetaData updatedAtCol = new ColumnMetaData(
                "updated_at",
                "Updated At",
                DomainFactory.fromInternalName("DATETIME"),
                updatedAtDef,
                Map.of()
        );

        ColumnMetaData summaryCol = new ColumnMetaData(
                "summary",
                "Summary",
                DomainFactory.fromInternalName("TEXT"),
                null,
                Map.of()
        );

        return List.of(idCol, summaryCol, createdAtCol, updatedAtCol);
    }
}
