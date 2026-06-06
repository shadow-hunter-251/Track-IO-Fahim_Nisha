package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByStudentId(Long studentId);
}
