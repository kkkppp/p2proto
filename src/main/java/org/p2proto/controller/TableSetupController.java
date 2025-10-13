package org.p2proto.controller;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.CurrentUser;
import org.p2proto.dto.TableMetadata;
import org.p2proto.repository.table.TableRepository;
import org.p2proto.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for creating/updating table metadata (immutable TableMetadata).
 */
@Controller
@RequestMapping("/tableSetup")
@Slf4j
public class TableSetupController {

    private final TableService tableService;
    private final TableRepository tableRepository;

    public TableSetupController(TableService tableService, TableRepository tableRepository) {
        this.tableService = tableService;
        this.tableRepository = tableRepository;
    }

    @GetMapping("")
    public String listTables(Model model) {
        List<TableMetadata> metadataList = tableRepository.findAllWithLabels();
        model.addAttribute("metadataList", metadataList);
        return "tableSetup/list";
    }

    @GetMapping("/create")
    public String createTable(Model model) {
        TableMetadata emptyMeta = TableMetadata.builder()
                .id(null)
                .tableName("")
                .tableLabel("")
                .tablePluralLabel("")
                .tableType(TableMetadata.TableTypeEnum.STANDARD)
                .columns(List.of())
                .primaryKeyMeta(null) // inferred later
                .build();
        model.addAttribute("table", emptyMeta);
        return "tableSetup/main";
    }

    @GetMapping("/edit/{id}")
    public String updateTable(@PathVariable("id") UUID id, Model model) {
        TableMetadata table = tableRepository.findByID(id);
        model.addAttribute("table", table);
        return "tableSetup/main";
    }

    @GetMapping("/fields/{id}")
    public String tableFields(@PathVariable("id") UUID id, Model model) {
        TableMetadata table = tableRepository.findByID(id);
        model.addAttribute("table", table);
        return "tableSetup/fieldsList";
    }

    /**
     * Save handler: binds to a mutable form DTO, then maps to immutable TableMetadata.
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveTable(@ModelAttribute("table") TableForm form,
                                                         @ModelAttribute("currentUser") CurrentUser currentUser) {

        // Ensure columns exist on create or when form posts none
        List<ColumnMetaData> cols = (form.getColumns() == null || form.getColumns().isEmpty())
                ? TableService.defaultColumns()
                : form.getColumns();

        // Optional explicit PK from UI; otherwise TableMetadata will infer
        ColumnMetaData pkMeta = null;
        if (form.getPrimaryKeyName() != null && !form.getPrimaryKeyName().isBlank()) {
            String pkName = form.getPrimaryKeyName();
            pkMeta = cols.stream()
                    .filter(c -> c.getName().equals(pkName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Primary key column not found: " + pkName));
        }

        TableMetadata meta = TableMetadata.builder()
                .id(form.getId())
                .tableName(Objects.requireNonNullElse(form.getTableName(), "").trim())
                .tableLabel(Objects.requireNonNullElse(form.getTableLabel(), "").trim())
                .tablePluralLabel(Objects.requireNonNullElse(form.getTablePluralLabel(), "").trim())
                .tableType(Objects.requireNonNullElse(form.getTableType(), TableMetadata.TableTypeEnum.STANDARD))
                .columns(cols)
                .primaryKeyMeta(pkMeta)
                .build();

        if (meta.getId() == null) {
            tableService.createTable(meta, currentUser);
        } else {
            tableRepository.updateMetadataInDb(meta);
        }

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("redirectUrl", "tableSetup");
        return ResponseEntity.ok(response);
    }

    /**
     * Form-backing bean so Spring can bind request params before we build the immutable TableMetadata.
     * If you prefer, move this to its own file.
     */
    public static class TableForm {
        private UUID id;
        private String tableName;
        private String tableLabel;
        private String tablePluralLabel;
        private TableMetadata.TableTypeEnum tableType;
        private List<ColumnMetaData> columns;
        private String primaryKeyName;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableLabel() { return tableLabel; }
        public void setTableLabel(String tableLabel) { this.tableLabel = tableLabel; }
        public String getTablePluralLabel() { return tablePluralLabel; }
        public void setTablePluralLabel(String tablePluralLabel) { this.tablePluralLabel = tablePluralLabel; }
        public TableMetadata.TableTypeEnum getTableType() { return tableType; }
        public void setTableType(TableMetadata.TableTypeEnum tableType) { this.tableType = tableType; }
        public List<ColumnMetaData> getColumns() { return columns; }
        public void setColumns(List<ColumnMetaData> columns) { this.columns = columns; }
        public String getPrimaryKeyName() { return primaryKeyName; }
        public void setPrimaryKeyName(String primaryKeyName) { this.primaryKeyName = primaryKeyName; }
    }
}
