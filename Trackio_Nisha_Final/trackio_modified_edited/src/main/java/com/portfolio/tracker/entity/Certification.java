package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String certificationId;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private String title;

    private String issuingOrganization;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String certificateLink;

    @Column(nullable = false)
    private boolean verified = false;

    public Certification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) { this.certificationId = certificationId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIssuingOrganization() { return issuingOrganization; }
    public void setIssuingOrganization(String issuingOrganization) { this.issuingOrganization = issuingOrganization; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getCertificateLink() { return certificateLink; }
    public void setCertificateLink(String certificateLink) { this.certificateLink = certificateLink; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Certification c = new Certification();
        public Builder certificationId(String v) { c.certificationId = v; return this; }
        public Builder student(Student v) { c.student = v; return this; }
        public Builder title(String v) { c.title = v; return this; }
        public Builder issuingOrganization(String v) { c.issuingOrganization = v; return this; }
        public Builder issueDate(LocalDate v) { c.issueDate = v; return this; }
        public Builder expiryDate(LocalDate v) { c.expiryDate = v; return this; }
        public Builder certificateLink(String v) { c.certificateLink = v; return this; }
        public Builder verified(boolean v) { c.verified = v; return this; }
        public Certification build() { return c; }
    }
}
