package org.p2proto.model.record;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class RecordForm {

    // Dynamic form fields
    private List<FormField> fields;

    // Constructors
    public RecordForm() {
    }

    public RecordForm(List<FormField> fields) {
        this.fields = fields;
    }
}
