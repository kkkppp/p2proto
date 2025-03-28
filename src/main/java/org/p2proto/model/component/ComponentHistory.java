package org.p2proto.model.component;

import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class ComponentHistory {

    public enum ComponentHistoryStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    private Long id;                        // BIGINT (PK, auto-increment)
    private UUID componentId;               // UUID references components.id
    private Long parentId;                  // BIGINT references component_history.id
    private ComponentHistoryStatus status;  // Enum
    private Integer userId;                 // references users.id
    private Timestamp timestamp;            // timestamp without time zone
    private String ddlStatement;            // TEXT
    private String oldState;                // JSONB stored as String
    private String newState;                // JSONB stored as String

    // Constructors
    public ComponentHistory() {
    }

    public ComponentHistory(Long id,
                            UUID componentId,
                            Long parentId,
                            ComponentHistoryStatus status,
                            Integer userId,
                            Timestamp timestamp,
                            String ddlStatement,
                            String oldState,
                            String newState) {
        this.id = id;
        this.componentId = componentId;
        this.parentId = parentId;
        this.status = status;
        this.userId = userId;
        this.timestamp = timestamp;
        this.ddlStatement = ddlStatement;
        this.oldState = oldState;
        this.newState = newState;
    }
}
