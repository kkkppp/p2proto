package org.p2proto.controller;

import org.p2proto.entity.ExtendedUser;
import org.p2proto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    public UserController() {
        log.info("UserController instantiated");
    }

    @GetMapping
    public String listUsers(Model model) {
        log.info("entered listusers");
        List<ExtendedUser> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "userList";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        log.info("Entered createUserForm");
        ExtendedUser user = new ExtendedUser();
        model.addAttribute("user", user);
        return "userCreate"; // Ensure this view exists
    }

    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable String id, Model model) {
        log.info("entered editusers");
        ExtendedUser user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "userEdit";
    }

    @PostMapping("/save")
    public String saveUser(@Valid @ModelAttribute ExtendedUser user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            if (user.getId() == null || user.getId().isEmpty()) {
                return "userCreate";
            } else {
                return "userEdit";
            }
        }

        if (user.getId() == null || user.getId().isEmpty()) {
            log.info("Saving new user: {}", user.getUsername());
            userService.saveUser(user);
        } else {
            log.info("Updating existing user with ID: {}", user.getId());
            userService.updateUser(user);
        }
        return "redirect:/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}
