package com.portfolio.tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String skillId;

    @Column(nullable = false)
    private String skillName;

    private String category;

    @Enumerated(EnumType.STRING)
    private Level level;

    private String linkedCourse;

    /** The student who added this skill (FK to students.id). */
    private Long studentId;

    /** Teacher verifies this skill is demonstrated via a project */
    @Column(nullable = false)
    private boolean verified = false;

    /** Optional: reference the project that evidences this skill */
    private String linkedProjectId;

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }

    public Skill() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public String getLinkedCourse() { return linkedCourse; }
    public void setLinkedCourse(String linkedCourse) { this.linkedCourse = linkedCourse; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getLinkedProjectId() { return linkedProjectId; }
    public void setLinkedProjectId(String linkedProjectId) { this.linkedProjectId = linkedProjectId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Skill s = new Skill();
        public Builder skillId(String v) { s.skillId = v; return this; }
        public Builder studentId(Long v) { s.studentId = v; return this; }
        public Builder skillName(String v) { s.skillName = v; return this; }
        public Builder category(String v) { s.category = v; return this; }
        public Builder level(Level v) { s.level = v; return this; }
        public Builder linkedCourse(String v) { s.linkedCourse = v; return this; }
        public Builder verified(boolean v) { s.verified = v; return this; }
        public Builder linkedProjectId(String v) { s.linkedProjectId = v; return this; }
        public Skill build() { return s; }
    }
}
