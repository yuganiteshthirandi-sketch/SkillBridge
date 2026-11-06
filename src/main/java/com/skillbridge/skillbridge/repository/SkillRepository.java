package com.skillbridge.skillbridge.repository;

import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Retrieve skills belonging to a particular user
    List<Skill> findByUser(User user);

    // Retrieve skills by category
    List<Skill> findByCategory(String category);

    // Retrieve skills by proficiency level
    List<Skill> findByProficiencyLevel(String proficiencyLevel);

    // Search skills by title
    List<Skill> findByTitleContainingIgnoreCase(String title);

    // Retrieve skills by user and category
    List<Skill> findByUserAndCategory(User user, String category);

    // Find a skill only if it belongs to the specified user
    Optional<Skill> findBySkillIdAndUser(Long skillId, User user);

    // Retrieve skills offered by other users
    List<Skill> findByUserNot(User user);

    // Search skills offered by other users
    List<Skill> findByUserNotAndTitleContainingIgnoreCase(User user, String title);

    // Filter skills offered by other users by category
    List<Skill> findByUserNotAndCategory(User user, String category);
}

