package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.CourseOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseOutcomeRepository extends JpaRepository<CourseOutcome, Long> {
    List<CourseOutcome> findByCourseIdOrderByCodeAsc(Long courseId);
    long countByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
}
