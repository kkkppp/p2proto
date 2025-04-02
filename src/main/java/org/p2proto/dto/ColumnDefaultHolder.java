package org.p2proto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefaultHolder {

    public enum DefaultValueType {
        CONSTANT,
        FORMULA
    }

    public enum ExecutionContext {
        SERVER_SIDE,
        CLIENT_SIDE
    }

    public enum TriggerEvent {
        ON_CREATE,
        ON_UPDATE,
        ON_ANY_CHANGE,
        ALWAYS
    }

    private DefaultValueType valueType;
    private String value;           // Constant (e.g., "42") or formula (e.g., "now()")
    private ExecutionContext executionContext;
    private TriggerEvent triggerEvent;

    // JSON Serialization (manual methods remain)
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public static ColumnDefaultHolder fromJson(String json) throws JsonProcessingException {
        if (json == null || json.isEmpty()) { return null; }
        return new ObjectMapper().readValue(json, ColumnDefaultHolder.class);
    }
}
