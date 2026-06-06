package com.portfolio.tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "CSE-3711" */
    @Column(unique = true, nullable = false, length = 30)
    private String courseCode;

    /** e.g. "Software Engineering" */
    @Column(nullable = false)
    private String courseName;

    /** Department offering the course. */
    private String department;

    /** Credit hours. */
    private Integer credits;

    /** Optional: who teaches it (free text, since teachers may not be in AppUser yet). */
    private String instructor;

    public Course() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
}
