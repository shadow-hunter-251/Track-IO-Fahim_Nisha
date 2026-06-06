package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);
    List<Course> findByDepartmentOrderByCourseCodeAsc(String department);
    List<Course> findAllByOrderByCourseCodeAsc();
}
