package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByStudentId(Long studentId);
    List<Skill> findByCategory(String category);
    List<Skill> findByLevel(Skill.Level level);
}
