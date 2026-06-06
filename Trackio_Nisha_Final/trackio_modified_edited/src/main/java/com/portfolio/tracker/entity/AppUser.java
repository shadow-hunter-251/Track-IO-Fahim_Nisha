package com.portfolio.tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Long studentId;

    // ---- Teacher profile fields ----
    private String teacherName;
    private String teacherDepartment;
    private String teacherEmail;
    private String teacherPhone;
    private String teacherSpecialization;

    @Column(length = 1000)
    private String teacherAddress;

    @Column(length = 500)
    private String teacherPhotoUrl;

    @Column(length = 500)
    private String teacherLinkedinUrl;

    private String teacherDesignation;

    private String teacherOffice;

    @Column(length = 2000)
    private String teacherBio;

    @Column(length = 500)
    private String teacherWebsite;

    @Column(length = 500)
    private String teacherGithubUrl;

    public enum Role { STUDENT, TEACHER }

    public AppUser() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getTeacherDepartment() { return teacherDepartment; }
    public void setTeacherDepartment(String teacherDepartment) { this.teacherDepartment = teacherDepartment; }
    public String getTeacherEmail() { return teacherEmail; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public String getTeacherPhone() { return teacherPhone; }
    public void setTeacherPhone(String teacherPhone) { this.teacherPhone = teacherPhone; }
    public String getTeacherSpecialization() { return teacherSpecialization; }
    public void setTeacherSpecialization(String teacherSpecialization) { this.teacherSpecialization = teacherSpecialization; }
    public String getTeacherAddress() { return teacherAddress; }
    public void setTeacherAddress(String teacherAddress) { this.teacherAddress = teacherAddress; }
    public String getTeacherPhotoUrl() { return teacherPhotoUrl; }
    public void setTeacherPhotoUrl(String teacherPhotoUrl) { this.teacherPhotoUrl = teacherPhotoUrl; }
    public String getTeacherLinkedinUrl() { return teacherLinkedinUrl; }
    public void setTeacherLinkedinUrl(String teacherLinkedinUrl) { this.teacherLinkedinUrl = teacherLinkedinUrl; }
    public String getTeacherDesignation() { return teacherDesignation; }
    public void setTeacherDesignation(String teacherDesignation) { this.teacherDesignation = teacherDesignation; }
    public String getTeacherOffice() { return teacherOffice; }
    public void setTeacherOffice(String teacherOffice) { this.teacherOffice = teacherOffice; }
    public String getTeacherBio() { return teacherBio; }
    public void setTeacherBio(String teacherBio) { this.teacherBio = teacherBio; }
    public String getTeacherWebsite() { return teacherWebsite; }
    public void setTeacherWebsite(String teacherWebsite) { this.teacherWebsite = teacherWebsite; }
    public String getTeacherGithubUrl() { return teacherGithubUrl; }
    public void setTeacherGithubUrl(String teacherGithubUrl) { this.teacherGithubUrl = teacherGithubUrl; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AppUser u = new AppUser();
        public Builder username(String v) { u.username = v; return this; }
        public Builder password(String v) { u.password = v; return this; }
        public Builder role(Role v) { u.role = v; return this; }
        public Builder studentId(Long v) { u.studentId = v; return this; }
        public Builder teacherName(String v) { u.teacherName = v; return this; }
        public Builder teacherDepartment(String v) { u.teacherDepartment = v; return this; }
        public Builder teacherEmail(String v) { u.teacherEmail = v; return this; }
        public Builder teacherPhone(String v) { u.teacherPhone = v; return this; }
        public Builder teacherSpecialization(String v) { u.teacherSpecialization = v; return this; }
        public Builder teacherAddress(String v) { u.teacherAddress = v; return this; }
        public Builder teacherPhotoUrl(String v) { u.teacherPhotoUrl = v; return this; }
        public Builder teacherLinkedinUrl(String v) { u.teacherLinkedinUrl = v; return this; }
        public Builder teacherDesignation(String v) { u.teacherDesignation = v; return this; }
        public Builder teacherOffice(String v) { u.teacherOffice = v; return this; }
        public Builder teacherBio(String v) { u.teacherBio = v; return this; }
        public Builder teacherWebsite(String v) { u.teacherWebsite = v; return this; }
        public Builder teacherGithubUrl(String v) { u.teacherGithubUrl = v; return this; }
        public AppUser build() { return u; }
    }
}
