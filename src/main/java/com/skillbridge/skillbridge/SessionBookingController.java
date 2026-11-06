package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.SessionBooking;
import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.repository.UserRepository;
import com.skillbridge.skillbridge.repository.SessionBookingRepository;
import com.skillbridge.skillbridge.repository.ReviewRepository;
import com.skillbridge.skillbridge.service.SkillService;
import com.skillbridge.skillbridge.service.SessionBookingService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class SessionBookingController {

    private final SessionBookingService sessionBookingService;
    private final SkillService skillService;
    private final UserRepository userRepository;
    private final SessionBookingRepository sessionBookingRepository;
    private final ReviewRepository reviewRepository;

    public SessionBookingController(
            SessionBookingService sessionBookingService,
            SkillService skillService,
            UserRepository userRepository,
            SessionBookingRepository sessionBookingRepository,
            ReviewRepository reviewRepository) {

        this.sessionBookingService = sessionBookingService;
        this.skillService = skillService;
        this.userRepository = userRepository;
        this.sessionBookingRepository = sessionBookingRepository;
        this.reviewRepository = reviewRepository;
    }

    // =========================================
    // SESSION BOOKING PAGE
    // =========================================

    @GetMapping("/sessions/book/{skillId}")
    public String bookingPage(
            @PathVariable Long skillId,
            Authentication authentication,
            Model model) {

        User learner = getLoggedInUser(authentication);
        Skill skill = skillService.getSkillById(skillId);

        if (skill.getUser().getUserId().equals(learner.getUserId())) {
            return "redirect:/skills";
        }

        model.addAttribute("skill", skill);
        model.addAttribute("teacher", skill.getUser());
        model.addAttribute("learner", learner);
        model.addAttribute("username", learner.getUsername());
        model.addAttribute("appearance", learner.getAppearance());
        return "session-booking";
    }

    // =========================================
    // CREATE SESSION BOOKING
    // =========================================

    @PostMapping("/sessions/book/{skillId}")
    public String createBooking(
            @PathVariable Long skillId,
            Authentication authentication,
            @RequestParam LocalDate sessionDate,
            @RequestParam LocalTime startTime,
            @RequestParam Integer durationHours,
            @RequestParam(required = false) String instructions,
            Model model,
            RedirectAttributes redirectAttributes) {

        User learner = getLoggedInUser(authentication);
        Skill skill = skillService.getSkillById(skillId);

        if (skill.getUser().getUserId().equals(learner.getUserId())) {
            return "redirect:/skills";
        }

        if (sessionDate.isBefore(LocalDate.now())) {

            model.addAttribute(
                    "errorMessage",
                    "Please select a future date.");

            addBookingPageAttributes(model, skill, learner);

            return "session-booking";
        }

        if (durationHours == null || durationHours < 1 || durationHours > 8) {

            model.addAttribute(
                    "errorMessage",
                    "Session duration must be between 1 and 8 hours.");

            addBookingPageAttributes(model, skill, learner);

            return "session-booking";
        }

        if (startTime == null) {

            model.addAttribute(
                    "errorMessage",
                    "Please select a valid start time.");

            addBookingPageAttributes(model, skill, learner);

            return "session-booking";
        }

        SessionBooking booking = new SessionBooking();

        booking.setLearner(learner);
        booking.setSkill(skill);
        booking.setSessionDate(sessionDate);
        booking.setStartTime(startTime);
        booking.setDurationHours(durationHours);
        booking.setInstructions(instructions);

        sessionBookingService.createBooking(booking);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your session booking has been submitted successfully!");

        return "redirect:/sessions/my-bookings";
    }

    // =========================================
    // LEARNER: MY BOOKINGS
    // =========================================

    @GetMapping("/sessions/my-bookings")
    public String myBookings(
            Authentication authentication,
            Model model) {

        User learner = getLoggedInUser(authentication);

        List<SessionBooking> bookings =
                sessionBookingRepository.findByLearner(learner);

        // Find which bookings already have reviews
        Set<Long> reviewedBookingIds = bookings.stream()
                .filter(reviewRepository::existsByBooking)
                .map(SessionBooking::getBookingId)
                .collect(Collectors.toSet());

        model.addAttribute("bookings", bookings);
        model.addAttribute("reviewedBookingIds", reviewedBookingIds);
        model.addAttribute("username", learner.getUsername());
        model.addAttribute("appearance", learner.getAppearance());
        return "my-bookings";
    }

    // =========================================
    // TEACHER: INCOMING BOOKINGS
    // =========================================

    @GetMapping("/sessions/teacher-bookings")
    public String teacherBookings(
            Authentication authentication,
            Model model) {

        User teacher = getLoggedInUser(authentication);

        List<Skill> teacherSkills = skillService.getUserSkills(teacher);

        List<SessionBooking> bookings =
                sessionBookingRepository.findBySkillIn(teacherSkills);

        model.addAttribute("bookings", bookings);
        model.addAttribute("username", teacher.getUsername());
        model.addAttribute("appearance", teacher.getAppearance());
        return "teacher-bookings";
    }

    // =========================================
    // TEACHER: ACCEPT, REJECT OR COMPLETE BOOKING
    // =========================================

    @PostMapping("/sessions/teacher-bookings/{bookingId}/status")
    public String updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User teacher = getLoggedInUser(authentication);

        SessionBooking booking = sessionBookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getSkill().getUser().getUserId()
                .equals(teacher.getUserId())) {

            return "redirect:/dashboard";
        }

        String newStatus = status.toUpperCase();

        if (!newStatus.equals("ACCEPTED")
                && !newStatus.equals("REJECTED")
                && !newStatus.equals("COMPLETED")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid booking status.");

            return "redirect:/sessions/teacher-bookings";
        }

        if (newStatus.equals("COMPLETED")
                && !booking.getStatus().equalsIgnoreCase("ACCEPTED")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only accepted sessions can be marked as completed.");

            return "redirect:/sessions/teacher-bookings";
        }

        if (booking.getStatus().equalsIgnoreCase("COMPLETED")
                || booking.getStatus().equalsIgnoreCase("REJECTED")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This booking can no longer be updated.");

            return "redirect:/sessions/teacher-bookings";
        }

        booking.setStatus(newStatus);

        sessionBookingRepository.save(booking);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Booking status updated to " + newStatus + ".");

        return "redirect:/sessions/teacher-bookings";
    }

    // =========================================
    // BOOKING PAGE MODEL ATTRIBUTES
    // =========================================

    private void addBookingPageAttributes(
            Model model,
            Skill skill,
            User learner) {

        model.addAttribute("skill", skill);
        model.addAttribute("teacher", skill.getUser());
        model.addAttribute("learner", learner);
        model.addAttribute("username", learner.getUsername());
        model.addAttribute("appearance", learner.getAppearance());
    }

    // =========================================
    // GET LOGGED-IN USER
    // =========================================

    private User getLoggedInUser(Authentication authentication) {

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}