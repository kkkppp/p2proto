package org.p2proto.controller;

import org.p2proto.entity.ExtendedUser;
import org.p2proto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

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
    public String editUser(@PathVariable UUID id, Model model) {
        log.info("entered editusers");
        ExtendedUser user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "userEdit";
    }

    @PostMapping("/save")
    public String saveUser(@Valid @ModelAttribute ExtendedUser user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            if (user.getId() == null) {
                return "userCreate";
            } else {
                return "userEdit";
            }
        }
        if (user.getPassword() != null) {
            user.setPasswordHash(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10)));
        }
        if (user.getId() == null) {
            log.info("Saving new user: {}", user.getUsername());
            userService.saveUser(user);
        } else {
            log.info("Updating existing user with ID: {}", user.getId());
            userService.updateUser(user);
        }
        return "redirect:/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}
