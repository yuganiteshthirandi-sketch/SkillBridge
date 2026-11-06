package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.service.AuthService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Open Login Page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Open Registration Page
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Process Registration Form
    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute User user,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        // Check whether passwords match
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "register";
        }

        try {
            // Save user to database
            authService.registerUser(user);

            // Redirect after successful registration
            return "redirect:/login?registered";

        } catch (RuntimeException e) {

            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}