package org.p2proto.model.component;

import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class Component {

    public enum ComponentTypeEnum {
        TABLE,
        FIELD,
        PAGE,
        ELEMENT
    }

    public enum ComponentStatusEnum {
        ACTIVE,
        LOCKED,
        INACTIVE
    }

    private UUID id;
    private ComponentTypeEnum componentType;
    private ComponentStatusEnum status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer createdBy;
    private Integer updatedBy;

    public Component() {
    }

    public Component(UUID id,
                     ComponentTypeEnum componentType,
                     ComponentStatusEnum status,
                     Timestamp createdAt,
                     Timestamp updatedAt,
                     Integer createdBy,
                     Integer updatedBy) {
        this.id = id;
        this.componentType = componentType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }
}
