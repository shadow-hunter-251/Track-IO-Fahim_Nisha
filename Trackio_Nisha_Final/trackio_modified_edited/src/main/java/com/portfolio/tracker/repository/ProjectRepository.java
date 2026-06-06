package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByStudentId(Long studentId);
    List<Project> findByProjectType(Project.ProjectType type);
}
