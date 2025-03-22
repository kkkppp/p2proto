package org.p2proto.controller;

import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.TableMetadata.ColumnMetaData;
import org.p2proto.model.record.FieldType;
import org.p2proto.model.record.FormField;
import org.p2proto.model.record.RecordForm;
import org.p2proto.repository.TableMetadataCrudRepository;
import org.p2proto.util.TableMetadataUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/table")
public class TableController {

    private final TableMetadataUtil util;
    private final HttpServletRequest request;

    public TableController(TableMetadataUtil util, HttpServletRequest request) {
        this.util = util;
        this.request = request;
    }
    /**
     * Prepares the model attributes required for displaying the table view.
     *
     * @param tableName The name of the table.
     * @param fieldsToRender Optional list of fields to render.
     * @param model The Spring Model object.
     */
    private void prepareTableViewModel(String tableName, List<String> fieldsToRender, Model model) {
        // Retrieve the TableMetadata for the given table name
        UUID tableID = util.findAll().get(tableName);
        TableMetadata tableMetadata = util.findByID(tableID);

        // Extract column information
        List<String> allColumns = tableMetadata.getColumns().stream()
                .map(ColumnMetaData::getName)
                .collect(Collectors.toList());

        // Retrieve all rows from the CRUD repository
        TableMetadataCrudRepository repo = new TableMetadataCrudRepository(util.getJdbcTemplate(), tableMetadata, "id");
        List<Map<String, Object>> records = repo.findAll();

        // Determine which fields to render
        List<String> fieldsToShow = (fieldsToRender == null || fieldsToRender.isEmpty()) ? allColumns : fieldsToRender;

        // Populate model attributes
        model.addAttribute("allFields", String.join(",", allColumns));
        model.addAttribute("fieldsToRender", String.join(",", fieldsToShow));
        model.addAttribute("columnLabels", tableMetadata.getColumns().stream()
                .collect(Collectors.toMap(ColumnMetaData::getName, ColumnMetaData::getLabel)));
        model.addAttribute("records", records);
        model.addAttribute("tableName", tableName);
        model.addAttribute("tableLabel", tableMetadata.getTableLabel());
        model.addAttribute("tableLabelPlural", tableMetadata.getTablePluralLabel());
    }

    /**
     * Displays the list of records for a given table.
     */
    @GetMapping("/{tableName}")
    public String listRecords(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "fields", required = false) List<String> fieldsToRender,
            Model model
    ) {
        if (fieldsToRender == null || fieldsToRender.isEmpty()) {
            fieldsToRender = applyTableView(tableName);
        }
        prepareTableViewModel(tableName, fieldsToRender, model);
        return "tableView";
    }

    private List<String> applyTableView(String tableName) {
        // TODO actual implementation
        if ("users".equals(tableName)) {
            return Arrays.asList("username","email","first_name","last_name");
        }
        return null;
    }

    @GetMapping("/{tableName}/create")
    public String createRecordForm(@PathVariable("tableName") String tableName, Model model) {
        // Reuse the same code, but pass null to indicate "no existing record"
        prepareRecordForm(tableName, null, model);
        model.addAttribute("mode", "create");
        return "createRecordContent"; // or a unified form view if you prefer
    }

    @GetMapping("/{tableName}/edit/{id}")
    public String editRecordForm(
            @PathVariable("tableName") String tableName,
            @PathVariable("id") String id,
            Model model
    ) {
        // Reuse the code and populate fields from the DB
        prepareRecordForm(tableName, id, model);
        model.addAttribute("mode", "edit");
        return "createRecordContent"; // or reuse "createRecordContent" if it’s the same view
    }

    /**
     * A private helper that:
     * 1) Retrieves the table metadata
     * 2) Builds a new RecordForm from the column definitions
     * 3) If recordId is provided, fetches existing data and populates the form fields
     * 4) Places the resulting RecordForm in the model.
     */
    private void prepareRecordForm(String tableName, String recordId, Model model) {
        // 1. Look up the metadata
        UUID tableID = util.findAll().get(tableName);
        TableMetadata tableMetadata = util.findByID(tableID);

        // 2. Build the empty (or default) form fields
        List<FormField> fields = tableMetadata.getColumns().stream()
                .map(ColumnMetaData::toFormField)
                .collect(Collectors.toList());

        // Create a RecordForm
        RecordForm recordForm = new RecordForm(fields);

        // 3. If editing, fetch existing record & populate fields
        if (recordId != null) {
            TableMetadataCrudRepository repo =
                    new TableMetadataCrudRepository(util.getJdbcTemplate(), tableMetadata, "id");

            Map<String, Object> recordData = repo.findById(Integer.valueOf(recordId));
            if (recordData != null) {
                // Populate each form field from existing DB data
                for (FormField field : fields) {
                    Object value = recordData.get(field.getName());
                    field.setValue(value == null ? "" : value.toString());
                }
            }

            model.addAttribute("recordId", recordId);
        }

        // 4. Populate the model
        model.addAttribute("tableName", tableName);
        model.addAttribute("tableLabel", tableMetadata.getTableLabel());
        model.addAttribute("record", recordForm);
    }

    /**
     * Handles the form submission to save a new record.
     */
    @PostMapping("/{tableName}/save")
    public ResponseEntity<Map<String, String>> saveRecord(
            @PathVariable("tableName") String tableName,
            @ModelAttribute RecordForm record,
            BindingResult result,
            Model model) {

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
            UUID tableID = util.findAll().get(tableName);
            TableMetadata tableMetadata = util.findByID(tableID);

            // Initialize the repository
            TableMetadataCrudRepository repo = new TableMetadataCrudRepository(util.getJdbcTemplate(), tableMetadata, "id");

            // Convert RecordForm to a Map<String, Object> for saving
            Map<String, Object> recordData = record.getFields().stream()
                    .collect(Collectors.toMap(FormField::getName, FormField::getValue));

            // Save the record (assuming 'id' is auto-generated
            repo.save(recordData);
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
