package org.p2proto.controller;

import org.p2proto.dto.TableMetadata;
import org.p2proto.util.TableMetadataUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/tableSetup")
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
}
