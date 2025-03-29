package org.p2proto.service;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.dto.TableMetadata;
import org.p2proto.model.component.Component;
import org.p2proto.model.component.ComponentHistory;
import org.p2proto.repository.component.ComponentHistoryRepository;
import org.p2proto.repository.component.ComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ComponentHistoryRepository historyRepository;

    public ComponentService(ComponentRepository componentRepository, ComponentHistoryRepository historyRepository) {
        this.componentRepository = componentRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Creates a locked component + in-progress history in one short transaction,
     * commits upon success or rolls back on exception.
     */
    @Transactional
    public Component createLockedComponentAndHistory(TableMetadata tableMetadata, Integer userId) {
        // Create a new Component with status=LOCKED
        Component component = new Component();
        component.setComponentType(Component.ComponentTypeEnum.TABLE);
        component.setStatus(Component.ComponentStatusEnum.LOCKED);
        component.setCreatedAt(Timestamp.from(Instant.now()));
        component.setCreatedBy(userId);

        componentRepository.save(component);
        tableMetadata.setId(component.getId());

        // Create a ComponentHistory entry with status=IN_PROGRESS
        ComponentHistory history = new ComponentHistory();
        history.setComponentId(component.getId());
        history.setStatus(ComponentHistory.ComponentHistoryStatus.IN_PROGRESS);
        history.setUserId(userId);
        history.setTimestamp(Timestamp.from(Instant.now()));
        history.setOldState("{}");
        history.setNewState("{}");

        historyRepository.save(history);

        // If no exception is thrown, this transaction commits automatically
        // at the end of the method. On RuntimeException, it rolls back.
        return component;
    }

    /**
     * Mark the component as ACTIVE and the most recent history entry as COMPLETED.
     * Creates records in tables and fields
     */

    @Transactional
    public void markSuccess(UUID componentId, List<String> ddl) {
        // 1) Update the component to ACTIVE
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(Component.ComponentStatusEnum.ACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        // 2) Update the latest history entry to COMPLETED
        var allHistories = historyRepository.findAll();
        allHistories.stream()
                .filter(h -> h.getComponentId().equals(componentId))
                .max((h1, h2) -> h1.getId().compareTo(h2.getId()))
                .ifPresent(h -> {
                    h.setStatus(ComponentHistory.ComponentHistoryStatus.COMPLETED);
                    h.setDdlStatement(String.join("\n", ddl));
                    historyRepository.update(h);
                });
        // transaction #3 commits here if no exception.
    }

    /**
     * Mark the component as INACTIVE and the most recent history entry as FAILED.
     */
    @Transactional
    public void markFailure(UUID componentId) {
        // 1) Update the component to INACTIVE
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(Component.ComponentStatusEnum.INACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        // 2) Update the latest history entry to FAILED
        var allHistories = historyRepository.findAll();
        allHistories.stream()
                .filter(h -> h.getComponentId().equals(componentId))
                .max((h1, h2) -> h1.getId().compareTo(h2.getId()))
                .ifPresent(h -> {
                    h.setStatus(ComponentHistory.ComponentHistoryStatus.FAILED);
                    historyRepository.update(h);
                });
    }

}
