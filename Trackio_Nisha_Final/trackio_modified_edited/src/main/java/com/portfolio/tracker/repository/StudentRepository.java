package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);
    List<Student> findByDepartment(String department);
    List<Student> findByBatch(String batch);
    boolean existsByEmail(String email);
    boolean existsByRegistrationNo(String registrationNo);
    boolean existsByStudentId(String studentId);
}
