package org.p2proto.controller;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.dto.CurrentUser;
import org.p2proto.dto.TableMetadata;
import org.p2proto.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tableSetup")
@Slf4j
public class TableSetupController {

    private final TableService tableService;

    public TableSetupController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("")
    public String listTables(Model model) {
        List<TableMetadata> metadataList = tableService.findAllWithLabels();
        model.addAttribute("metadataList", metadataList);
        return "tableSetup/list";
    }

    @GetMapping("/create")
    public String createTable(Model model) {
        model.addAttribute("table", new TableMetadata());
        return "tableSetup/create";
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveTable(@ModelAttribute("table") TableMetadata tableMetadata, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Map<String, String> response = new HashMap<>();

        if (tableMetadata.getColumns() == null) {
            tableMetadata.setColumns(TableService.defaultColumns());
        }
        tableService.createTable(tableMetadata, currentUser);

        response.put("status", "success");
        response.put("redirectUrl", "tableSetup");
        return ResponseEntity.ok(response);

    }

}
