package org.p2proto.controller;

import org.p2proto.model.MenuItem;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("menu")
    public List<MenuItem> populateMenu() {
        MenuItem tables = new MenuItem("Tables", "#", Arrays.asList(
                new MenuItem("Manage Users", "/table/users?fields=username,email,first_name,last_name")
        ));

        MenuItem access = new MenuItem("Access", "#", Arrays.asList(
                new MenuItem("Manage Groups", "/groups"),
                new MenuItem("Manage Teams", "/teams")
        ));

        return Arrays.asList(tables, access);
    }

    // Optionally, set the active menu based on the request
    @ModelAttribute("activeMenu")
    public String determineActiveMenu(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/users")) {
            return "Tables";
        } else if (uri.contains("/groups") || uri.contains("/teams")) {
            return "Access";
        }
        return "";
    }
}
