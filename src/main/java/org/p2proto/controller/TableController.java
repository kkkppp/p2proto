package org.p2proto.controller;

import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.TableMetadata.ColumnMetaData;
import org.p2proto.model.record.FormField;
import org.p2proto.model.record.RecordForm;
import org.p2proto.repository.TableMetadataCrudRepository;
import org.p2proto.util.TableMetadataUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/table")
public class TableController {

    private final TableMetadataUtil util;

    public TableController(TableMetadataUtil util) {
        this.util = util;
    }

    @GetMapping("/{tableName}")
    public String listRecords(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "fields", required = false) List<String> fieldsToRender,
            Model model
    ) {
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

        return "tableView";
    }

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
}
