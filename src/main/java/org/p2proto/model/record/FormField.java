package org.p2proto.model.record;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class FormField {
    private String name;
    private String label;
    private FieldType type;
    private boolean required;
    private List<String> options; // For SELECT, RADIO, CHECKBOX types
    private String value; // Holds the user-entered value
    private List<String> errors;

    // Constructors
    public FormField() {}

    public FormField(String name, String label, FieldType type, boolean required) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
    }

    public FormField(String name, String label, FieldType type, boolean required, List<String> options) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.options = options;
    }

}
