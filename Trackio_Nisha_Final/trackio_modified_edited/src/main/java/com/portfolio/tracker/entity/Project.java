package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String supervisor;

    @Enumerated(EnumType.STRING)
    private ProjectType projectType = ProjectType.ASSIGNMENT;

    /**
     * Course Outcomes this project contributes to.
     * Stored as comma-separated CO IDs: "1,3,7"
     */
    @Column(length = 200)
    private String courseOutcomeIds;

    /** Optional: link to a Course this project belongs to (FK to courses.id). */
    private Long courseId;

    public enum ProjectType { ASSIGNMENT, PROJECT, CAPSTONE }

    public Project() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }
    public String getCourseOutcomeIds() { return courseOutcomeIds; }
    public void setCourseOutcomeIds(String courseOutcomeIds) { this.courseOutcomeIds = courseOutcomeIds; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Project p = new Project();
        public Builder projectId(String v) { p.projectId = v; return this; }
        public Builder title(String v) { p.title = v; return this; }
        public Builder description(String v) { p.description = v; return this; }
        public Builder student(Student v) { p.student = v; return this; }
        public Builder courseName(String v) { p.courseName = v; return this; }
        public Builder startDate(LocalDate v) { p.startDate = v; return this; }
        public Builder endDate(LocalDate v) { p.endDate = v; return this; }
        public Builder supervisor(String v) { p.supervisor = v; return this; }
        public Builder projectType(ProjectType v) { p.projectType = v; return this; }
        public Project build() { return p; }
    }
}
