package com.skillbridge.skillbridge.service;

import com.skillbridge.skillbridge.entity.SessionBooking;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.entity.Skill;

import com.skillbridge.skillbridge.repository.SessionBookingRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SessionBookingService {

    private final SessionBookingRepository sessionBookingRepository;

    public SessionBookingService(
            SessionBookingRepository sessionBookingRepository) {

        this.sessionBookingRepository = sessionBookingRepository;
    }

    // Create a new session booking
    public SessionBooking createBooking(SessionBooking booking) {

        booking.setStatus("PENDING");

        return sessionBookingRepository.save(booking);
    }

    // Get booking by ID
    public Optional<SessionBooking> getBookingById(Long bookingId) {

        return sessionBookingRepository.findById(bookingId);
    }

    // Get all bookings made by a learner
    public List<SessionBooking> getLearnerBookings(User learner) {

        return sessionBookingRepository.findByLearner(learner);
    }

    // Get all bookings related to a skill
    public List<SessionBooking> getSkillBookings(Skill skill) {

        return sessionBookingRepository.findBySkill(skill);
    }

    // Get bookings by status
    public List<SessionBooking> getBookingsByStatus(String status) {

        return sessionBookingRepository.findByStatus(status);
    }

    // Get learner bookings by status
    public List<SessionBooking> getLearnerBookingsByStatus(
            User learner,
            String status) {

        return sessionBookingRepository.findByLearnerAndStatus(
                learner,
                status
        );
    }

    // Update booking status
    public SessionBooking updateBookingStatus(
            Long bookingId,
            String status) {

        Optional<SessionBooking> optionalBooking =
                sessionBookingRepository.findById(bookingId);

        if (optionalBooking.isPresent()) {

            SessionBooking booking = optionalBooking.get();

            booking.setStatus(status);

            return sessionBookingRepository.save(booking);
        }

        return null;
    }

    // Delete booking
    public void deleteBooking(Long bookingId) {

        sessionBookingRepository.deleteById(bookingId);
    }
}