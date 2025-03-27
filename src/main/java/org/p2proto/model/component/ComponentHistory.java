package org.p2proto.model.component;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ComponentHistory {

    public enum ComponentHistoryStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    private Long id;                       // BIGINT (PK, auto-increment)
    private UUID componentId;              // UUID references components.id
    private ComponentHistoryStatus status; // Enum
    private Integer userId;                // int references users.id
    private Timestamp timestamp;       // timestamp without time zone
    private String changeDetails;          // JSON stored as String

    // Constructors
    public ComponentHistory() {
    }

    public ComponentHistory(Long id, UUID componentId, ComponentHistoryStatus status,
                            Integer userId, Timestamp timestamp, String changeDetails) {
        this.id = id;
        this.componentId = componentId;
        this.status = status;
        this.userId = userId;
        this.timestamp = timestamp;
        this.changeDetails = changeDetails;
    }
}
