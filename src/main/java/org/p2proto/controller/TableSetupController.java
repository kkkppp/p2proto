package org.p2proto.controller;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.dto.TableMetadata;
import org.p2proto.util.TableMetadataUtil;
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

    private final TableMetadataUtil tableMetadataUtil;

    public TableSetupController(TableMetadataUtil tableMetadataUtil) {
        this.tableMetadataUtil = tableMetadataUtil;
    }

    @GetMapping("")
    public String listTables(Model model) {
        List<TableMetadata> metadataList = tableMetadataUtil.findAllWithLabels();
        model.addAttribute("metadataList", metadataList);
        return "tableSetup/list";
    }

    @GetMapping("/create")
    public String createTable(Model model) {
        model.addAttribute("table", new TableMetadata());
        return "tableSetup/create";
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>>  saveTable(@ModelAttribute("table") TableMetadata tableMetadata) {
        log.info("model = " + tableMetadata);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("redirectUrl", "tableSetup");
        return ResponseEntity.ok(response);

    }

}
