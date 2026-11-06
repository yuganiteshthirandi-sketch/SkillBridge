package com.skillbridge.skillbridge.repository;

import com.skillbridge.skillbridge.entity.SessionBooking;
import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionBookingRepository
        extends JpaRepository<SessionBooking, Long> {

    List<SessionBooking> findByLearner(User learner);

    List<SessionBooking> findBySkill(Skill skill);

    List<SessionBooking> findBySkillIn(List<Skill> skills);

    List<SessionBooking> findByStatus(String status);

    List<SessionBooking> findByLearnerAndStatus(
            User learner,
            String status);
}