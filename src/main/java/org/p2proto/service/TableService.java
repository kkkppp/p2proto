package org.p2proto.service;

import liquibase.exception.DatabaseException;
import org.p2proto.ddl.CreateTableCommand;
import org.p2proto.ddl.DDLExecutor;
import org.p2proto.dto.TableMetadata;
import org.p2proto.model.component.Component;
import org.p2proto.model.component.Component.ComponentStatusEnum;
import org.p2proto.model.component.ComponentHistory;
import org.p2proto.model.component.ComponentHistory.ComponentHistoryStatus;
import org.p2proto.repository.component.ComponentRepository;
import org.p2proto.repository.component.ComponentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class TableService {

    private final PlatformTransactionManager transactionManager;
    private final ComponentRepository componentRepository;
    private final ComponentHistoryRepository historyRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DDLExecutor ddlExecutor;

    @Autowired
    public TableService(PlatformTransactionManager transactionManager,
                        ComponentRepository componentRepository,
                        ComponentHistoryRepository historyRepository,
                        JdbcTemplate jdbcTemplate,
                        DDLExecutor ddlExecutor) {
        this.transactionManager = transactionManager;
        this.componentRepository = componentRepository;
        this.historyRepository = historyRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.ddlExecutor = ddlExecutor;
    }

    public void createTable(String tableName, String tableLabel, String tablePluralLabel) {
        createTableInternal(new CreateTableCommand(tableName, tableLabel, tablePluralLabel));
    }

    public void createTable(TableMetadata tableMetadata) {
        createTableInternal(new CreateTableCommand(tableMetadata));
    }

    private void createTableInternal(CreateTableCommand command) {
        Component component = null;
        try {
            component = createLockedComponentAndHistory(command.getTable());

            executeDDL(command);
            markSuccess(component.getId());
        } catch (DatabaseException | SQLException e) {
            if (component != null ) {
                markFailure(component.getId());
            }

        }
    }

    /**
     * FIRST TRANSACTION:
     * Creates a locked component + in-progress history in one short transaction,
     * commits upon success or rolls back on exception.
     */
    @Transactional
    public Component createLockedComponentAndHistory(TableMetadata tableMetadata) {
        // Create a new Component with status=LOCKED
        Component component = new Component();
        component.setComponentType(Component.ComponentTypeEnum.TABLE);
        component.setStatus(Component.ComponentStatusEnum.LOCKED);
        component.setCreatedAt(Timestamp.from(Instant.now()));
        // setCreatedBy(...) if needed

        componentRepository.save(component);

        // Create a ComponentHistory entry with status=IN_PROGRESS
        ComponentHistory history = new ComponentHistory();
        history.setComponentId(component.getId());
        history.setStatus(ComponentHistory.ComponentHistoryStatus.IN_PROGRESS);
        // setUserId(...) if needed
        history.setTimestamp(Timestamp.from(Instant.now()));
        history.setDdlStatement("-- DDL statement goes here");
        history.setOldState("{}");
        history.setNewState("{}");

        historyRepository.save(history);

        // If no exception is thrown, this transaction commits automatically
        // at the end of the method. On RuntimeException, it rolls back.
        return component;
    }

    /**
     * SECOND TRANSACTION (REQUIRED):
     * Runs the DDL in a separate transaction. If this fails, an exception is thrown and that transaction rolls back.
     */
    @Transactional
    public void executeDDL(CreateTableCommand command) throws SQLException, DatabaseException {
        ddlExecutor.executeDDL(command);
    }

    /**
     * THIRD TRANSACTION (REQUIRED) - SUCCESS PATH:
     * Mark the component as ACTIVE and the most recent history entry as COMPLETED.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void markSuccess(UUID componentId) {
        // 1) Update the component to ACTIVE
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(ComponentStatusEnum.ACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        // 2) Update the latest history entry to COMPLETED
        var allHistories = historyRepository.findAll();
        allHistories.stream()
                .filter(h -> h.getComponentId().equals(componentId))
                .max((h1, h2) -> h1.getId().compareTo(h2.getId()))
                .ifPresent(h -> {
                    h.setStatus(ComponentHistoryStatus.COMPLETED);
                    historyRepository.update(h);
                });
        // transaction #3 commits here if no exception.
    }

    /**
     * THIRD TRANSACTION (REQUIRES_NEW) - FAILURE PATH:
     * Mark the component as INACTIVE and the most recent history entry as FAILED.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void markFailure(UUID componentId) {
        // 1) Update the component to INACTIVE
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(ComponentStatusEnum.INACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        // 2) Update the latest history entry to FAILED
        var allHistories = historyRepository.findAll();
        allHistories.stream()
                .filter(h -> h.getComponentId().equals(componentId))
                .max((h1, h2) -> h1.getId().compareTo(h2.getId()))
                .ifPresent(h -> {
                    h.setStatus(ComponentHistoryStatus.FAILED);
                    historyRepository.update(h);
                });
    }
}
