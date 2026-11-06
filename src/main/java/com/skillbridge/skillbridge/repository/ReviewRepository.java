package com.skillbridge.skillbridge.repository;

import com.skillbridge.skillbridge.entity.Review;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.entity.SessionBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBooking(SessionBooking booking);

    boolean existsByBooking(SessionBooking booking);

    List<Review> findByTeacher(User teacher);

    List<Review> findByLearner(User learner);

    List<Review> findByTeacherOrderByCreatedAtDesc(User teacher);
}
