
package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

    private final UserRepository userRepository;

    public SettingsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/settings")
    public String settingsPage(Authentication authentication, Model model) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            Authentication authentication,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(defaultValue = "English") String preferredLanguage,
            @RequestParam(defaultValue = "Dark Mode") String appearance,
            @RequestParam(defaultValue = "false") boolean emailNotifications,
            @RequestParam(defaultValue = "false") boolean sessionReminders,
            @RequestParam(defaultValue = "false") boolean publicProfile,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        fullName = fullName.trim();
        username = username.trim();
        email = email.trim();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "All account information fields are required."
            );
            return "redirect:/settings";
        }

        if (!user.getUsername().equals(username)
                && userRepository.existsByUsername(username)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Username is already taken."
            );

            return "redirect:/settings";
        }

        if (!user.getEmail().equals(email)
                && userRepository.existsByEmail(email)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Email is already registered."
            );

            return "redirect:/settings";
        }

        if (!preferredLanguage.equals("English")
                && !preferredLanguage.equals("Hindi")
                && !preferredLanguage.equals("Gujarati")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid preferred language."
            );

            return "redirect:/settings";
        }

        if (!appearance.equals("Dark Mode")
                && !appearance.equals("Light Mode")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid appearance selection."
            );

            return "redirect:/settings";
        }

        // Update Account Information
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);

        // Update Preferences
        user.setPreferredLanguage(preferredLanguage);
        user.setAppearance(appearance);
        user.setEmailNotifications(emailNotifications);
        user.setSessionReminders(sessionReminders);
        user.setPublicProfile(publicProfile);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account information and preferences updated successfully!"
        );

        return "redirect:/settings";
    }
}
