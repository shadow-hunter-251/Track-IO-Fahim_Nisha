package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "internships")
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recordId;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String companyName;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Status status = Status.APPLIED;

    public enum Status { APPLIED, SELECTED, COMPLETED }

    public Internship() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Internship i = new Internship();
        public Builder recordId(String v) { i.recordId = v; return this; }
        public Builder student(Student v) { i.student = v; return this; }
        public Builder companyName(String v) { i.companyName = v; return this; }
        public Builder role(String v) { i.role = v; return this; }
        public Builder startDate(LocalDate v) { i.startDate = v; return this; }
        public Builder endDate(LocalDate v) { i.endDate = v; return this; }
        public Builder status(Status v) { i.status = v; return this; }
        public Internship build() { return i; }
    }
}
