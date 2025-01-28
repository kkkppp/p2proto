package org.p2proto.controller;

import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.TableMetadata.ColumnMetaData;
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
        prepareTableViewModel(tableName, fieldsToRender, model);
        return "tableView";
    }

    /**
     * Displays the form to create a new record.
     */
    @GetMapping("/{tableName}/create")
    public String createRecordForm(@PathVariable("tableName") String tableName, Model model) {
        UUID tableID = util.findAll().get(tableName);
        TableMetadata tableMetadata = util.findByID(tableID);

        // Build form fields based on the column metadata
        List<FormField> fields = tableMetadata.getColumns().stream()
                .map(ColumnMetaData::toFormField)
                .collect(Collectors.toList());

        RecordForm record = new RecordForm(fields);
        model.addAttribute("tableName", tableName);
        model.addAttribute("record", record);

        return "createRecordContent"; // Ensure this view exists
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
            //repo.save(recordData);
        } catch (Exception e) {
            // Handle exceptions (e.g., log the error)
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
