package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.entity.Review;
import com.skillbridge.skillbridge.repository.UserRepository;
import com.skillbridge.skillbridge.repository.ReviewRepository;
import com.skillbridge.skillbridge.service.SkillService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

@Controller
public class SkillController {

    private final SkillService skillService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public SkillController(
            SkillService skillService,
            UserRepository userRepository,
            ReviewRepository reviewRepository) {
        this.skillService = skillService;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    // =========================================
    // MY SKILLS PAGE
    // =========================================

    @GetMapping("/skills")
    public String mySkills(Authentication authentication, Model model) {
        User user = getLoggedInUser(authentication);

        model.addAttribute("skills", skillService.getUserSkills(user));
        model.addAttribute("username", user.getUsername());

        return "my-skills";
    }

    // =========================================
    // PHASE 7 + 13: EXPLORE SKILLS MARKETPLACE
    // PHASE 13: ADVANCED SEARCH, FILTERING & SORTING
    // =========================================

    @GetMapping("/skills/explore")
    public String exploreSkills(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String proficiencyLevel,
            @RequestParam(required = false) Double minRate,
            @RequestParam(required = false) Double maxRate,
            @RequestParam(required = false, defaultValue = "default") String sort,
            Model model) {

        User user = getLoggedInUser(authentication);

        // Retrieve skills using advanced filters
        List<Skill> skills = skillService.getFilteredExploreSkills(
                user,
                search,
                category,
                proficiencyLevel,
                minRate,
                maxRate);

        // =========================================
        // CALCULATE MENTOR RATINGS
        // =========================================

        List<Review> reviews = reviewRepository.findAll();

        Map<Long, Double> ratingTotals = new HashMap<>();
        Map<Long, Integer> reviewCounts = new HashMap<>();

        for (Review review : reviews) {

            if (review.getTeacher() == null || review.getRating() == null) {
                continue;
            }

            Long teacherId = review.getTeacher().getUserId();

            ratingTotals.put(
                    teacherId,
                    ratingTotals.getOrDefault(teacherId, 0.0)
                            + review.getRating());

            reviewCounts.put(
                    teacherId,
                    reviewCounts.getOrDefault(teacherId, 0) + 1);
        }

        Map<Long, Map<String, Object>> teacherRatings = new HashMap<>();

        for (Long teacherId : ratingTotals.keySet()) {

            double averageRating =
                    ratingTotals.get(teacherId)
                            / reviewCounts.get(teacherId);

            Map<String, Object> ratingData = new HashMap<>();

            ratingData.put("averageRating", averageRating);
            ratingData.put("reviewCount", reviewCounts.get(teacherId));

            teacherRatings.put(teacherId, ratingData);
        }

        // =========================================
        // SORT RESULTS
        // =========================================

        Comparator<Skill> comparator;

        switch (sort.toLowerCase()) {

            case "highest-rating":

                comparator = Comparator.comparingDouble(
                        skill -> getTeacherRating(skill, teacherRatings));

                comparator = comparator.reversed();

                break;

            case "rate-low-high":

                comparator = Comparator.comparing(
                        Skill::getHourlyRate,
                        Comparator.nullsLast(Comparator.naturalOrder()));

                break;

            case "rate-high-low":

                comparator = Comparator.comparing(
                        Skill::getHourlyRate,
                        Comparator.nullsLast(Comparator.reverseOrder()));

                break;

            case "name-a-z":

                comparator = Comparator.comparing(
                        Skill::getTitle,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

                break;

            default:

                comparator = Comparator.comparing(
                        Skill::getSkillId,
                        Comparator.nullsLast(Comparator.reverseOrder()));

                break;
        }

        skills.sort(comparator);

        // =========================================
        // PAGE ATTRIBUTES
        // =========================================

        model.addAttribute("skills", skills);
        model.addAttribute("teacherRatings", teacherRatings);
        model.addAttribute("username", user.getUsername());

        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedProficiencyLevel", proficiencyLevel);
        model.addAttribute("minRate", minRate);
        model.addAttribute("maxRate", maxRate);
        model.addAttribute("selectedSort", sort);

        model.addAttribute("categories", new String[]{
                "Programming",
                "Web Development",
                "App Development",
                "Design",
                "Data Science",
                "AI & ML",
                "Business",
                "Marketing",
                "Other"
        });

        model.addAttribute("proficiencyLevels", new String[]{
                "Beginner",
                "Intermediate",
                "Advanced"
        });

        return "explore-skills";
    }

    // =========================================
    // GET TEACHER RATING
    // =========================================

    private double getTeacherRating(
            Skill skill,
            Map<Long, Map<String, Object>> teacherRatings) {

        if (skill.getUser() == null) {
            return 0.0;
        }

        Long teacherId = skill.getUser().getUserId();

        Map<String, Object> ratingData = teacherRatings.get(teacherId);

        if (ratingData == null || ratingData.get("averageRating") == null) {
            return 0.0;
        }

        return ((Number) ratingData.get("averageRating")).doubleValue();
    }

    // =========================================
    // PHASE 8: SKILL DETAILS
    // =========================================

    @GetMapping("/skills/details/{id}")
    public String skillDetails(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User currentUser = getLoggedInUser(authentication);

        Skill skill = skillService.getSkillById(id);

        // Prevent users from viewing their own skill
        if (skill.getUser().getUserId()
                .equals(currentUser.getUserId())) {

            return "redirect:/skills";
        }

        model.addAttribute("skill", skill);
        model.addAttribute("teacher", skill.getUser());
        model.addAttribute("username", currentUser.getUsername());

        return "skill-details";
    }

    // =========================================
    // ADD SKILL PAGE
    // =========================================

    @GetMapping("/skills/add")
    public String addSkillPage(
            Authentication authentication,
            Model model) {

        User user = getLoggedInUser(authentication);

        model.addAttribute("username", user.getUsername());

        return "add-skill";
    }

    // =========================================
    // SAVE NEW SKILL
    // =========================================

    @PostMapping("/skills/add")
    public String saveSkill(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String proficiencyLevel,
            @RequestParam(required = false) Double hourlyRate,
            @RequestParam String description,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        if (title.trim().isEmpty()
                || category.trim().isEmpty()
                || proficiencyLevel.trim().isEmpty()
                || description.trim().isEmpty()
                || hourlyRate == null
                || hourlyRate <= 0
                || hourlyRate > 100000) {

            model.addAttribute("username", user.getUsername());

            model.addAttribute(
                    "error",
                    "Please enter valid details. Hourly rate must be between ₹1 and ₹100000.");

            return "add-skill";
        }

        Skill skill = new Skill();

        skill.setTitle(title.trim());
        skill.setCategory(category);
        skill.setProficiencyLevel(proficiencyLevel);
        skill.setHourlyRate(hourlyRate);
        skill.setDescription(description.trim());
        skill.setUser(user);

        skillService.addSkill(skill);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your skill has been successfully added!");

        return "redirect:/skills";
    }

    // =========================================
    // EDIT SKILL PAGE
    // =========================================

    @GetMapping("/skills/edit/{id}")
    public String editSkillPage(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User user = getLoggedInUser(authentication);

        Skill skill = skillService.getSkillByIdAndUser(id, user);

        model.addAttribute("skill", skill);
        model.addAttribute("username", user.getUsername());

        return "edit-skill";
    }

    // =========================================
    // UPDATE EXISTING SKILL
    // =========================================

    @PostMapping("/skills/edit/{id}")
    public String updateSkill(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String proficiencyLevel,
            @RequestParam(required = false) Double hourlyRate,
            @RequestParam String description,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        if (title.trim().isEmpty()
                || category.trim().isEmpty()
                || proficiencyLevel.trim().isEmpty()
                || description.trim().isEmpty()
                || hourlyRate == null
                || hourlyRate <= 0
                || hourlyRate > 100000) {

            Skill skill = skillService.getSkillByIdAndUser(id, user);

            skill.setTitle(title);
            skill.setCategory(category);
            skill.setProficiencyLevel(proficiencyLevel);
            skill.setHourlyRate(hourlyRate);
            skill.setDescription(description);

            model.addAttribute("skill", skill);
            model.addAttribute("username", user.getUsername());

            model.addAttribute(
                    "error",
                    "Please enter valid details. Hourly rate must be between ₹1 and ₹100000.");

            return "edit-skill";
        }

        skillService.updateSkill(
                id,
                user,
                title.trim(),
                description.trim(),
                category,
                proficiencyLevel,
                hourlyRate);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your skill has been successfully updated!");

        return "redirect:/skills";
    }

    // =========================================
    // DELETE SKILL
    // =========================================

    @PostMapping("/skills/delete/{id}")
    public String deleteSkill(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        skillService.deleteSkill(id, user);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your skill has been successfully deleted!");

        return "redirect:/skills";
    }

    // =========================================
    // GET LOGGED-IN USER
    // =========================================

    private User getLoggedInUser(Authentication authentication) {

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}