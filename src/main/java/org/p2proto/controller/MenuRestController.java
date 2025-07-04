package org.p2proto.controller;

import org.p2proto.dto.CurrentUser;
import org.p2proto.model.MenuItem;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.List;
// ... other imports

@RestController
public class MenuRestController {

    private final GlobalControllerAdvice globalControllerAdvice;

    public MenuRestController(GlobalControllerAdvice globalControllerAdvice) {
        this.globalControllerAdvice = globalControllerAdvice;
    }

    @GetMapping("/api/menu-data")
    public MenuDataResponse getMenuData(HttpServletRequest request, HttpSession session) {
        // We can reuse the same logic from GlobalControllerAdvice:
        var menu = globalControllerAdvice.populateMenu();
        var activeMenu = globalControllerAdvice.determineActiveMenu(request);
        var currentUser = globalControllerAdvice.populateCurrentUser(
                (OAuth2AuthenticationToken) request.getUserPrincipal(),
                session
        );

        // Return a simple DTO (POJO) that wraps these values
        return new MenuDataResponse(menu, activeMenu, currentUser);
    }

    /**
     * A small DTO class. It will be returned as JSON to React client.
     */
    public static class MenuDataResponse {
        private final List<MenuItem> menu;
        private final String activeMenu;
        private final CurrentUser currentUser;

        public MenuDataResponse(List<MenuItem> menu, String activeMenu, CurrentUser currentUser) {
            this.menu = menu;
            this.activeMenu = activeMenu;
            this.currentUser = currentUser;
        }

        public List<MenuItem> getMenu() { return menu; }
        public String getActiveMenu() { return activeMenu; }
        public CurrentUser getCurrentUser() { return currentUser; }
    }
}
