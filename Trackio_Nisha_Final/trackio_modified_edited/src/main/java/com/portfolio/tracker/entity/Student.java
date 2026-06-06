package com.portfolio.tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String name;

    private String department;

    @Column(unique = true)
    private String registrationNo;

    private String batch;
    private String email;
    private String phone;

    @Column(length = 1000)
    private String address;

    @Column(unique = true)
    private String username;
    private String password;

    // Portfolio fields (added for premium signup)
    @Column(length = 500)
    private String profilePhotoUrl;

    @Column(length = 500)
    private String githubUrl;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 500)
    private String portfolioUrl;

    @Column(length = 2000)
    private String bio;

    @Column(length = 1000)
    private String skills;

    @Column(length = 500)
    private String cvUrl;

    @Enumerated(EnumType.STRING)
    private Role role = Role.STUDENT;

    public enum Role { STUDENT, TEACHER }

    public Student() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }
    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Student s = new Student();
        public Builder studentId(String v) { s.studentId = v; return this; }
        public Builder name(String v) { s.name = v; return this; }
        public Builder department(String v) { s.department = v; return this; }
        public Builder registrationNo(String v) { s.registrationNo = v; return this; }
        public Builder batch(String v) { s.batch = v; return this; }
        public Builder email(String v) { s.email = v; return this; }
        public Builder phone(String v) { s.phone = v; return this; }
        public Builder address(String v) { s.address = v; return this; }
        public Builder username(String v) { s.username = v; return this; }
        public Builder password(String v) { s.password = v; return this; }
        public Builder profilePhotoUrl(String v) { s.profilePhotoUrl = v; return this; }
        public Builder githubUrl(String v) { s.githubUrl = v; return this; }
        public Builder linkedinUrl(String v) { s.linkedinUrl = v; return this; }
        public Builder portfolioUrl(String v) { s.portfolioUrl = v; return this; }
        public Builder bio(String v) { s.bio = v; return this; }
        public Builder skills(String v) { s.skills = v; return this; }
        public Builder cvUrl(String v) { s.cvUrl = v; return this; }
        public Builder role(Role v) { s.role = v; return this; }
        public Student build() { return s; }
    }
}
