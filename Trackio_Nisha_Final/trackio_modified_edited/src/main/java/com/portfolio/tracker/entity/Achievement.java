package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String achievementId;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private String eventName;

    private String position;
    private String organizer;
    private LocalDate date;
    private String proofLink;

    @Column(nullable = false)
    private boolean verified = false;

    public Achievement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getProofLink() { return proofLink; }
    public void setProofLink(String proofLink) { this.proofLink = proofLink; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Achievement a = new Achievement();
        public Builder achievementId(String v) { a.achievementId = v; return this; }
        public Builder student(Student v) { a.student = v; return this; }
        public Builder eventName(String v) { a.eventName = v; return this; }
        public Builder position(String v) { a.position = v; return this; }
        public Builder organizer(String v) { a.organizer = v; return this; }
        public Builder date(LocalDate v) { a.date = v; return this; }
        public Builder proofLink(String v) { a.proofLink = v; return this; }
        public Builder verified(boolean v) { a.verified = v; return this; }
        public Achievement build() { return a; }
    }
}
