package org.p2proto.controller;

import org.p2proto.dto.TableMetadata;
import org.p2proto.repository.TableMetadataCrudRepository;
import org.p2proto.util.TableMetadataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/table")
public class TableController {

    private final TableMetadataUtil util;

    public TableController(TableMetadataUtil util) {
        this.util = util;
    }
    @GetMapping("/{tableName}")
    public String listUsers(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "fields", required = false) List<String> fieldsToRender,
            Model model
    ) {
        // 1) Suppose you retrieve the TableMetadata for the "users" table
        UUID tableID = util.findAll().get(tableName);
        TableMetadata tableMetadata = util.findByID(tableID);
        List<String> allColumns = tableMetadata.getColumnNames();

        // 2) Retrieve all user rows from your CRUD repository as maps
        TableMetadataCrudRepository repo = new TableMetadataCrudRepository(util.getJdbcTemplate(), tableMetadata, "id");
        List<Map<String, Object>> users = repo.findAll();

        // 3) If fieldsToRender is null or empty, we’ll render all columns
        //    Otherwise, we’ll just render the subset
        model.addAttribute("allFields", allColumns);
        model.addAttribute("fieldsToRender", fieldsToRender);
        model.addAttribute("users", users);

        // Return the name of the JSP (assuming we have a view resolver that maps to /WEB-INF/jsp/ by default)
        return "userList";
    }

/*
        @GetMapping("/{tableName}")
        public String handleTable(
                @PathVariable("tableName") String tableName,
                Model model) {

            return "tableView"; // The name of your view (e.g., JSP, Thymeleaf)
        }
*/
}
