package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.Review;
import com.skillbridge.skillbridge.entity.SessionBooking;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.repository.ReviewRepository;
import com.skillbridge.skillbridge.repository.SessionBookingRepository;
import com.skillbridge.skillbridge.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final SessionBookingRepository sessionBookingRepository;
    private final UserRepository userRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            SessionBookingRepository sessionBookingRepository,
            UserRepository userRepository) {

        this.reviewRepository = reviewRepository;
        this.sessionBookingRepository = sessionBookingRepository;
        this.userRepository = userRepository;
    }

    // =========================================
    // LEARNER: SUBMIT SESSION REVIEW
    // =========================================

    @PostMapping("/sessions/{bookingId}/review")
    public String submitReview(
            @PathVariable Long bookingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String feedback,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User learner = getLoggedInUser(authentication);

        SessionBooking booking = sessionBookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verify that the logged-in user is the learner
        if (!booking.getLearner().getUserId()
                .equals(learner.getUserId())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You are not authorized to review this session.");

            return "redirect:/sessions/my-bookings";
        }

        // Only completed sessions can be reviewed
        if (booking.getStatus() == null
                || !booking.getStatus().equalsIgnoreCase("COMPLETED")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You can review only completed sessions.");

            return "redirect:/sessions/my-bookings";
        }

        // Validate rating
        if (rating == null || rating < 1 || rating > 5) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Rating must be between 1 and 5 stars.");

            return "redirect:/sessions/my-bookings";
        }

        // Prevent duplicate reviews
        if (reviewRepository.existsByBooking(booking)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You have already reviewed this session.");

            return "redirect:/sessions/my-bookings";
        }

        Review review = new Review();

        review.setBooking(booking);
        review.setLearner(learner);
        review.setTeacher(booking.getSkill().getUser());
        review.setRating(rating);
        review.setFeedback(feedback);

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your review has been submitted successfully!");

        return "redirect:/sessions/my-bookings";
    }

    // =========================================
    // GET LOGGED-IN USER
    // =========================================

    private User getLoggedInUser(Authentication authentication) {

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
