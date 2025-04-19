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
        // Forward the request to React's entry point which is now served as a static resource.
        return "forward:/index.html";
    }

    @GetMapping("/ajax/sidebar")
    public String loadSidebar() {
        // If this sidebar is still needed by React, adjust its implementation appropriately.
        return "includes/sidebar"; // This still assumes a JSP view; update if not needed.
    }
}
