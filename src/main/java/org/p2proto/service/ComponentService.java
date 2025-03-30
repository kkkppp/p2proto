package org.p2proto.service;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.model.component.Component;
import org.p2proto.model.component.ComponentHistory;
import org.p2proto.repository.component.ComponentHistoryRepository;
import org.p2proto.repository.component.ComponentRepository;
import org.springframework.stereotype.Service;
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

    @Transactional
    public Component createComponent(Component.ComponentTypeEnum componentType, Component.ComponentStatusEnum componentStatus, Integer userId) {
        Component component = new Component();
        component.setComponentType(componentType);
        component.setStatus(componentStatus);
        component.setCreatedAt(Timestamp.from(Instant.now()));
        component.setCreatedBy(userId);
        componentRepository.save(component);
        return component;
    }

    @Transactional
    public Long createHistory(UUID componentId, ComponentHistory.ComponentHistoryStatus status, Integer userId) {
        ComponentHistory history = new ComponentHistory();
        history.setComponentId(componentId);
        history.setStatus(status);
        history.setUserId(userId);
        history.setTimestamp(Timestamp.from(Instant.now()));
        history.setOldState("{}");
        history.setNewState("{}");

        return historyRepository.save(history);
    }

    /**
     * Mark the component as ACTIVE and the most recent history entry as COMPLETED.
     */

    @Transactional
    public void markSuccess(UUID componentId, Long historyId, List<String> ddl) {
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(Component.ComponentStatusEnum.ACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        var historyOpt = historyRepository.findById(historyId);
        historyOpt.ifPresent(h -> {
                    h.setStatus(ComponentHistory.ComponentHistoryStatus.COMPLETED);
                    h.setDdlStatement(String.join("\n", ddl));
                    historyRepository.update(h);
                });
    }

    /**
     * Mark the component as INACTIVE and the history entry as FAILED.
     */
    @Transactional
    public void markFailure(UUID componentId, Long historyId) {
        var compOpt = componentRepository.findById(componentId);
        compOpt.ifPresent(c -> {
            c.setStatus(Component.ComponentStatusEnum.INACTIVE);
            c.setUpdatedAt(Timestamp.from(Instant.now()));
            componentRepository.update(c);
        });

        var historyOpt = historyRepository.findById(historyId);
        historyOpt.ifPresent(h -> {
            h.setStatus(ComponentHistory.ComponentHistoryStatus.FAILED);
            historyRepository.update(h);
        });
    }

}
