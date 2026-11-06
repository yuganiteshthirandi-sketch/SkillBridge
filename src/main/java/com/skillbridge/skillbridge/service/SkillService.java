package com.skillbridge.skillbridge.service;

import com.skillbridge.skillbridge.entity.Skill;
import com.skillbridge.skillbridge.entity.User;
import com.skillbridge.skillbridge.repository.SkillRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    // Add a new skill
    public Skill addSkill(Skill skill) {
        return skillRepository.save(skill);
    }

    // Get all skills belonging to a user
    public List<Skill> getUserSkills(User user) {
        return skillRepository.findByUser(user);
    }

    // Get all skills
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    // Get skill by ID
    public Skill getSkillById(Long skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
    }

    // Get skill by ID and owner
    public Skill getSkillByIdAndUser(Long skillId, User user) {
        return skillRepository.findBySkillIdAndUser(skillId, user)
                .orElseThrow(() -> new RuntimeException(
                        "Skill not found or you do not have permission to access it"));
    }

    // Update a skill securely
    public Skill updateSkill(
            Long skillId,
            User user,
            String title,
            String description,
            String category,
            String proficiencyLevel,
            Double hourlyRate) {

        Skill skill = getSkillByIdAndUser(skillId, user);

        skill.setTitle(title);
        skill.setDescription(description);
        skill.setCategory(category);
        skill.setProficiencyLevel(proficiencyLevel);
        skill.setHourlyRate(hourlyRate);

        return skillRepository.save(skill);
    }

    // Delete a skill securely
    public void deleteSkill(Long skillId, User user) {

        Skill skill = getSkillByIdAndUser(skillId, user);

        skillRepository.delete(skill);
    }

    // Search skills by title
    public List<Skill> searchSkills(String title) {
        return skillRepository.findByTitleContainingIgnoreCase(title);
    }

    // Get skills by category
    public List<Skill> getSkillsByCategory(String category) {
        return skillRepository.findByCategory(category);
    }

    // =========================================
    // EXPLORE SKILLS MARKETPLACE
    // =========================================

    // Get skills offered by other users
    public List<Skill> getExploreSkills(User currentUser) {
        return skillRepository.findByUserNot(currentUser);
    }

    // Search skills offered by other users
    public List<Skill> searchExploreSkills(User currentUser, String title) {
        return skillRepository.findByUserNotAndTitleContainingIgnoreCase(
                currentUser, title);
    }

    // Filter skills offered by other users by category
    public List<Skill> filterExploreSkills(User currentUser, String category) {
        return skillRepository.findByUserNotAndCategory(
                currentUser, category);
    }

    // =========================================
    // PHASE 13: ADVANCED SKILL FILTERING
    // =========================================

    public List<Skill> getFilteredExploreSkills(
            User currentUser,
            String search,
            String category,
            String proficiencyLevel,
            Double minRate,
            Double maxRate) {

        List<Skill> skills = getExploreSkills(currentUser);

        return skills.stream()

                // Search by skill title
                .filter(skill ->
                        search == null
                        || search.trim().isEmpty()
                        || skill.getTitle()
                                .toLowerCase()
                                .contains(search.trim().toLowerCase()))

                // Filter by category
                .filter(skill ->
                        category == null
                        || category.trim().isEmpty()
                        || category.equalsIgnoreCase("All")
                        || skill.getCategory()
                                .equalsIgnoreCase(category))

                // Filter by proficiency level
                .filter(skill ->
                        proficiencyLevel == null
                        || proficiencyLevel.trim().isEmpty()
                        || proficiencyLevel.equalsIgnoreCase("All")
                        || skill.getProficiencyLevel()
                                .equalsIgnoreCase(proficiencyLevel))

                // Filter by minimum hourly rate
                .filter(skill ->
                        minRate == null
                        || (skill.getHourlyRate() != null
                        && skill.getHourlyRate() >= minRate))

                // Filter by maximum hourly rate
                .filter(skill ->
                        maxRate == null
                        || (skill.getHourlyRate() != null
                        && skill.getHourlyRate() <= maxRate))

                .collect(Collectors.toList());
    }
}
