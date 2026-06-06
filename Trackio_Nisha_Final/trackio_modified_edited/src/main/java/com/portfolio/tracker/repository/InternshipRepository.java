package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByStudentId(Long studentId);
    List<Internship> findByStatus(Internship.Status status);
}
