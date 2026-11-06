package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {

        // Default appearance for visitors
        model.addAttribute("appearance", "Dark Mode");

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElse(null);

            if (user != null) {
                model.addAttribute("username", user.getUsername());
                model.addAttribute("appearance", user.getAppearance());
            }
        }

        return "index";
    }
}