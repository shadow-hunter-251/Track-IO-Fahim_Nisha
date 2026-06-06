package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a student's request for a teacher to endorse one of their claimed skills.
 * Once a teacher approves, the linked Skill flips its `verified` flag to true and the
 * endorsement record carries the teacher's identity + optional proficiency rating + note.
 */
@Entity
@Table(name = "skill_endorsements")
public class SkillEndorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the Skill being endorsed (FK to skills.id). */
    @Column(nullable = false)
    private Long skillId;

    /** ID of the Student who owns the skill (FK to students.id). */
    @Column(nullable = false)
    private Long studentId;

    /** ID of the teacher AppUser this endorsement is requested from. */
    @Column(nullable = false)
    private Long teacherUserId;

    /** Snapshot fields so the endorsement is readable even if names change later. */
    private String studentName;
    private String teacherName;
    private String skillName;

    /** Student-supplied note explaining what they did to demonstrate the skill. */
    @Column(length = 1500)
    private String studentNote;

    /** Optional: project ID that evidences the claim. */
    private String evidenceProjectId;

    /** Teacher's response. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    private Proficiency proficiency;

    @Column(length = 1500)
    private String teacherNote;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime respondedAt;

    public enum Status { PENDING, APPROVED, DECLINED }
    public enum Proficiency { BEGINNER, PROFICIENT, ADVANCED, EXPERT }

    public SkillEndorsement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getTeacherUserId() { return teacherUserId; }
    public void setTeacherUserId(Long teacherUserId) { this.teacherUserId = teacherUserId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getStudentNote() { return studentNote; }
    public void setStudentNote(String studentNote) { this.studentNote = studentNote; }
    public String getEvidenceProjectId() { return evidenceProjectId; }
    public void setEvidenceProjectId(String evidenceProjectId) { this.evidenceProjectId = evidenceProjectId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Proficiency getProficiency() { return proficiency; }
    public void setProficiency(Proficiency proficiency) { this.proficiency = proficiency; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
