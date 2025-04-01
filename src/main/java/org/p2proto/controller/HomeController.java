package org.p2proto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showIndex(Principal principal, Model model) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);
        }
        return "index";  // Maps to index.jsp
    }

    @GetMapping("/ajax/sidebar")
    public String loadSidebar() {
        return "includes/sidebar"; // Resolves to /WEB-INF/views/includes/sidebar.jsp
    }

}
