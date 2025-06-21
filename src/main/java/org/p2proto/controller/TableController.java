package org.p2proto.controller;

import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;
import org.p2proto.model.record.FieldType;
import org.p2proto.model.record.FormField;
import org.p2proto.model.record.RecordForm;
import org.p2proto.repository.TableMetadataCrudRepository;
import org.p2proto.repository.table.TableRepository;
import org.p2proto.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/table")
public class TableController {

    private final TableService tableService;
    private final TableRepository tableRepository;
    private final HttpServletRequest request;

    public TableController(TableService tableService, TableRepository tableRepository, HttpServletRequest request) {
        this.tableService = tableService;
        this.tableRepository = tableRepository;
        this.request = request;
    }

    private Map<String, Object> buildTableData(String tableName, List<String> fieldsToRender) {
        // Retrieve the TableMetadata for the given table name
        UUID tableID = tableRepository.findAll().get(tableName);
        TableMetadata tableMetadata = tableRepository.findByID(tableID);

        // Extract all column names from the metadata
        List<String> allColumns = tableMetadata.getColumns().stream()
                .map(ColumnMetaData::getName)
                .collect(Collectors.toList());

        // Retrieve all rows from the CRUD repository
        TableMetadataCrudRepository repo = new TableMetadataCrudRepository(tableService.getJdbcTemplate(), tableMetadata, "id");
        List<Map<String, Object>> records = repo.findAll();

        // Determine which fields to render: if none provided, render all columns
        List<String> fieldsToShow = (fieldsToRender == null || fieldsToRender.isEmpty())
                ? allColumns : fieldsToRender;

        // Build a map of column labels (keyed by column name)
        Map<String, String> columnLabels = tableMetadata.getColumns().stream()
                .collect(Collectors.toMap(ColumnMetaData::getName, ColumnMetaData::getLabel));


        Map<String, String> cellFormatters = tableMetadata.getColumns().stream()
                .filter(col -> {
                    String t = col.getDomain().getInternalName();              // e.g. "TIMESTAMP" or "DATE"
                    return "DATETIME".equalsIgnoreCase(t) || "DATE".equalsIgnoreCase(t);
                })
                .collect(Collectors.toMap(
                        ColumnMetaData::getName,
                        col -> "date"                          // frontend will interpret "date" →  new Date(ms).toLocaleString()
                ));
        // Prepare the data map to send to the React component
        Map<String, Object> data = new HashMap<>();
        data.put("allFields", allColumns);  // array of all column names
        data.put("fieldsToRender", fieldsToShow);  // array of fields to display
        data.put("columnLabels", columnLabels);
        data.put("records", records);
        data.put("tableName", tableName);
        data.put("tableLabel", tableMetadata.getTableLabel());
        data.put("tableLabelPlural", tableMetadata.getTablePluralLabel());
        data.put("contextPath", request.getContextPath());
        data.put("cellFormatters", cellFormatters);
        return data;
    }

    /**
     * Displays the list of records for a given table.
     */
    @GetMapping("/{tableName}")
    public ResponseEntity<Map<String, Object>> listRecords(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "fields", required = false) List<String> fieldsToRender
    ) {
        if (fieldsToRender == null || fieldsToRender.isEmpty()) {
            fieldsToRender = applyTableView(tableName);
        }
        Map<String, Object> data = buildTableData(tableName, fieldsToRender);
        return ResponseEntity.ok(data);
    }

    private List<String> applyTableView(String tableName) {
        // TODO actual implementation
        if ("users".equals(tableName)) {
            return Arrays.asList("username", "email", "first_name", "last_name");
        }
        return null;
    }

    @GetMapping(
            value = "/{tableName}/create",
            produces = "application/json"
    )
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRecordData(
            @PathVariable String tableName) {

        Map<String, Object> payload = prepareRecordPayload(tableName, null);
        payload.put("mode", "create");
        return ResponseEntity.ok(payload);
    }

    @GetMapping(
            value = "/{tableName}/{id}/edit",
            produces = "application/json"
    )
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editRecordData(
            @PathVariable String tableName,
            @PathVariable String id) {

        Map<String, Object> payload = prepareRecordPayload(tableName, id);
        payload.put("mode", "edit");
        payload.put("recordId", id);
        return ResponseEntity.ok(payload);
    }

    private Map<String, Object> prepareRecordPayload(String tableName,
                                                     String recordId) {

        UUID tableID = tableRepository.findAll().get(tableName);
        TableMetadata meta = tableRepository.findByID(tableID);

        // Build FormField list
        List<FormField> fields = meta.getColumns().stream()
                .map(ColumnMetaData::toFormField)
                .collect(Collectors.toList());

        // Populate values when editing
        if (recordId != null) {
            TableMetadataCrudRepository repo =
                    new TableMetadataCrudRepository(tableService.getJdbcTemplate(), meta, "id");

            Map<String, Object> db = repo.findById(Integer.parseInt(recordId));
            if (db != null) {
                fields.forEach(f -> {
                    Object v = db.get(f.getName());
                    f.setValue(v == null ? "" : v.toString());
                });
            }
        }

        RecordForm recordForm = new RecordForm(fields);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tableName", tableName);
        payload.put("tableLabel", meta.getTableLabel());
        payload.put("record", recordForm);

        return payload;
    }

    @PostMapping(value = "/{tableName}/create", consumes = "application/json")
    public ResponseEntity<Map<String, String>> createRecord(
            @PathVariable String tableName,
            @RequestBody RecordForm record,        // JSON → RecordForm (needs @JsonCreator in RecordForm)
            BindingResult result) {

        return saveRecord(tableName, record, result, null);    // delegate
    }

    @PostMapping(value = "/{tableName}/{id}/edit", consumes = "application/json")
    public ResponseEntity<Map<String, String>> updateRecord(
            @PathVariable String tableName,
            @PathVariable String id,
            @RequestBody RecordForm record,
            BindingResult result) {

        return saveRecord(tableName, record, result, id);      // delegate
    }

    public ResponseEntity<Map<String, String>> saveRecord(
            @PathVariable("tableName") String tableName,
            @ModelAttribute RecordForm record,
            BindingResult result,
            String id) {

        Map<String, String> response = new HashMap<>();

        normalizeCheckboxValues(record);
        // Perform custom validation if needed
        validateUserForm(record, result);

        if (result.hasErrors()) {
            // Collect error messages
            String errorMessages = result.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            response.put("status", "error");
            response.put("message", errorMessages);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Retrieve TableMetadata based on tableName
            UUID tableID = tableRepository.findAll().get(tableName);
            TableMetadata tableMetadata = tableRepository.findByID(tableID);

            // Initialize the repository
            TableMetadataCrudRepository repo = new TableMetadataCrudRepository(tableService.getJdbcTemplate(), tableMetadata, "id");

            // Convert RecordForm to a Map<String, Object> for saving
            Map<String, Object> recordData = record.getFields().stream()
                    .collect(Collectors.toMap(FormField::getName, f -> Objects.requireNonNullElse(f.getValue(), "") ));

            // Save the record (assuming 'id' is auto-generated
            repo.save(id, recordData);
        } catch (Exception e) {
            // Handle exceptions (e.g., log the error)
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to save the record. Please try again.");
            return ResponseEntity.status(500).body(response);
        }

        // On successful save, provide the URL to fetch the updated list of records
        String contextPath = request.getContextPath();
        String redirectUrl = contextPath + "/table/" + tableName;
        response.put("status", "success");
        response.put("redirectUrl", redirectUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a record by its ID for a given table.
     */
    @PostMapping("/{tableName}/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteRecord(
            @PathVariable("tableName") String tableName,
            @PathVariable("id") String id
    ) {
        Map<String, String> response = new HashMap<>();
        try {
            // 1. Retrieve table metadata
            UUID tableID = tableRepository.findAll().get(tableName);
            TableMetadata tableMetadata = tableRepository.findByID(tableID);

            // 2. Initialize repository
            TableMetadataCrudRepository repo =
                    new TableMetadataCrudRepository(tableService.getJdbcTemplate(), tableMetadata, "id");

            // 3. Perform the delete
            repo.delete(Integer.valueOf(id));

            // If successful, return success response
            String contextPath = request.getContextPath();
            String redirectUrl = contextPath + "/table/" + tableName;
            response.put("status", "success");
            response.put("redirectUrl", redirectUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to delete the record. Please try again.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Helper method that sets checkbox fields to "false" if they are null or empty.
     * This ensures no null values appear in the final recordData.
     */
    private void normalizeCheckboxValues(RecordForm recordForm) {
        for (FormField field : recordForm.getFields()) {
            // Check if this field is a checkbox AND is null/empty
            if (FieldType.CHECKBOX.equals(field.getType()) &&
                    (field.getValue() == null || field.getValue().trim().isEmpty())) {
                field.setValue("false");
            }
        }
    }

    /**
     * Validates the RecordForm. Customize this method based on your validation needs.
     */
    private void validateUserForm(RecordForm record, BindingResult result) {
        // Example validation: ensure required fields are not empty
        for (FormField field : record.getFields()) {
            if (field.isRequired() && (field.getValue() == null || field.getValue().trim().isEmpty())) {
                result.rejectValue("fields[" + record.getFields().indexOf(field) + "].value", "field.required", field.getLabel() + " is required.");
            }

            // Add more validation rules based on field types or other criteria
            switch (field.getType()) {
                case EMAIL:
                    if (field.getValue() != null && !field.getValue().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        result.rejectValue("fields[" + record.getFields().indexOf(field) + "].value", "field.invalid", "Invalid email format.");
                    }
                    break;
                case PASSWORD:
                    if (field.getValue() != null && field.getValue().length() < 6) {
                        result.rejectValue("fields[" + record.getFields().indexOf(field) + "].value", "field.invalid", "Password must be at least 6 characters long.");
                    }
                    break;
                // Add more cases as needed
                default:
                    break;
            }
        }
    }
}
