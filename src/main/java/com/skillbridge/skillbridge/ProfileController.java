package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.entity.Review;
import com.skillbridge.skillbridge.repository.UserRepository;
import com.skillbridge.skillbridge.repository.ReviewRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public ProfileController(
            UserRepository userRepository,
            ReviewRepository reviewRepository) {

        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    // =========================================
    // VIEW PROFILE
    // =========================================

    @GetMapping("/profile")
    public String profile(
            Authentication authentication,
            Model model) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        // Profile Completion
        int completion = 0;

        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            completion += 25;
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            completion += 25;
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            completion += 25;
        }

        if (user.getAccountType() != null && !user.getAccountType().isBlank()) {
            completion += 25;
        }

        model.addAttribute("profileCompletion", completion);

        String completionStyle =
                "background: conic-gradient(#8b5cf6 0% " + completion
                + "%, rgba(255,255,255,0.07) " + completion + "% 100%)";

        model.addAttribute("profileCompletionStyle", completionStyle);

        // =========================================
        // MENTOR REVIEWS AND RATINGS
        // =========================================

        List<Review> reviews =
                reviewRepository.findByTeacherOrderByCreatedAtDesc(user);

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("totalReviews", reviews.size());

        return "profile";
    }

    // =========================================
    // DISPLAY EDIT PROFILE FORM
    // =========================================

    @GetMapping("/profile/edit")
    public String editProfile(
            Authentication authentication,
            Model model) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        return "profile-edit";
    }

    // =========================================
    // UPDATE PROFILE
    // =========================================

    @PostMapping("/profile/edit")
    public String updateProfile(
            Authentication authentication,
            @ModelAttribute User formUser,
            @RequestParam String fullName,
            @RequestParam String email,
            Model model,
            RedirectAttributes redirectAttributes) {

        User existingUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String updatedName = fullName.trim();
        String updatedEmail = email.trim();

        if (updatedName.isEmpty()
                || updatedEmail.isEmpty()
                || !updatedEmail.contains("@")) {

            model.addAttribute("user", existingUser);

            model.addAttribute(
                    "error",
                    "Please enter a valid name and email address.");

            return "profile-edit";
        }

        boolean emailAlreadyUsed = userRepository.findByEmail(updatedEmail)
                .filter(user ->
                        !user.getUserId().equals(existingUser.getUserId()))
                .isPresent();

        if (emailAlreadyUsed) {

            model.addAttribute("user", existingUser);

            model.addAttribute(
                    "error",
                    "This email address is already registered.");

            return "profile-edit";
        }

        existingUser.setFullName(updatedName);
        existingUser.setEmail(updatedEmail);

        userRepository.save(existingUser);

        redirectAttributes.addFlashAttribute(
                "success",
                "Your profile has been updated successfully!");

        return "redirect:/profile";
    }
}