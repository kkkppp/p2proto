package org.p2proto.controller;

import org.p2proto.dto.CurrentUser;
import org.p2proto.model.MenuItem;
import org.p2proto.util.UserUtil;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserUtil userUtil;

    public GlobalControllerAdvice(UserUtil userUtil) {
        this.userUtil = userUtil;
    }

    @ModelAttribute("menu")
    public List<MenuItem> populateMenu() {
        MenuItem setup = new MenuItem("Setup", "#", Arrays.asList(
                new MenuItem("Tables", "/tableSetup")
        ));

        MenuItem tables = new MenuItem("Tables", "#", Arrays.asList(
                new MenuItem("Manage Users", "/table/users?fields=username,email,first_name,last_name")
        ));

        MenuItem access = new MenuItem("Access", "#", Arrays.asList(
                new MenuItem("Manage Groups", "/groups"),
                new MenuItem("Manage Teams", "/teams")
        ));

        return Arrays.asList(tables, setup, access);
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

    @ModelAttribute("currentUser")
    public CurrentUser populateCurrentUser(OAuth2AuthenticationToken token, HttpSession session) {
        if (token == null) {
            return null;
        }
        // Check if we already have user in session
        CurrentUser cached = (CurrentUser) session.getAttribute("myUserData");
        if (cached != null) {
            return cached;
        }
        // If not in session yet, query DB now:
        if (token.getPrincipal() instanceof OidcUser oidcUser) {
            String subject = oidcUser.getSubject();
            String[] tmp = subject.split(":");
            if (tmp.length == 3) {
                UUID uuid = UUID.fromString(tmp[2]);
                CurrentUser userFromDb = userUtil.findUserByUuid(uuid);
                session.setAttribute("myUserData", userFromDb);
                return userFromDb;
            }
        }
        return null;
    }
}
