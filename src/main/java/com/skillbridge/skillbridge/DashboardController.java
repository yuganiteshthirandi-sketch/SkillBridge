
package com.skillbridge.skillbridge;

import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.SessionBooking;

import com.skillbridge.skillbridge.repository.UserRepository;
import com.skillbridge.skillbridge.repository.SessionBookingRepository;

import com.skillbridge.skillbridge.service.SkillService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final SkillService skillService;
    private final SessionBookingRepository sessionBookingRepository;

    public DashboardController(
            UserRepository userRepository,
            SkillService skillService,
            SessionBookingRepository sessionBookingRepository) {

        this.userRepository = userRepository;
        this.skillService = skillService;
        this.sessionBookingRepository = sessionBookingRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Learner bookings
        List<SessionBooking> learnerBookings =
                sessionBookingRepository.findByLearner(user);

        Set<Long> learningSkillIds = learnerBookings.stream()
                .filter(booking -> booking.getSkill() != null)
                .map(booking -> booking.getSkill().getSkillId())
                .collect(Collectors.toSet());

        int skillsLearning = learningSkillIds.size();

        // Teacher bookings
        List<Skill> teacherSkills =
                skillService.getUserSkills(user);

        int skillsTeaching = teacherSkills.size();

        List<SessionBooking> teacherBookings =
                sessionBookingRepository.findBySkillIn(teacherSkills);

        // Total sessions
        int totalSessions =
                learnerBookings.size() + teacherBookings.size();

        // Total earnings
        double totalEarnings = teacherBookings.stream()
                .filter(booking -> "COMPLETED".equalsIgnoreCase(
                        booking.getStatus()))
                .filter(booking -> booking.getSkill() != null)
                .filter(booking -> booking.getDurationHours() != null)
                .filter(booking -> booking.getSkill().getHourlyRate() != null)
                .mapToDouble(booking ->
                        booking.getSkill().getHourlyRate()
                                * booking.getDurationHours())
                .sum();

        // Recent activity
        List<SessionBooking> recentActivity = new ArrayList<>();

        recentActivity.addAll(learnerBookings);
        recentActivity.addAll(teacherBookings);

        recentActivity = recentActivity.stream()
                .distinct()
                .sorted(Comparator.comparing(
                        SessionBooking::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());

        // Dashboard model
        model.addAttribute("username", user.getUsername());

        // Saved appearance preference
        model.addAttribute("appearance", user.getAppearance());

        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("skillsLearning", skillsLearning);
        model.addAttribute("skillsTeaching", skillsTeaching);
        model.addAttribute("totalEarnings", totalEarnings);

        model.addAttribute("recentActivity", recentActivity);

        return "dashboard";
    }
}
