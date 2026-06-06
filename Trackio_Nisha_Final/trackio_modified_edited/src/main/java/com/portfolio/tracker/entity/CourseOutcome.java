package com.portfolio.tracker.entity;

import jakarta.persistence.*;

/**
 * Course Outcome -- what students should achieve after completing a course.
 * Each CO is mapped to one or more Program Outcomes with weights (1=low, 2=medium, 3=high).
 *
 * Mappings are stored as a simple string: "PO1:3,PO3:2,PO5:1"
 * Keeps the schema flat and easy to inspect/edit; no join table noise.
 */
@Entity
@Table(name = "course_outcomes")
public class CourseOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning course (FK). */
    @Column(nullable = false)
    private Long courseId;

    /** e.g. "CO1", "CO2"... */
    @Column(nullable = false, length = 10)
    private String code;

    /** The CO statement -- what students will be able to do. */
    @Column(nullable = false, length = 1500)
    private String statement;

    /** Bloom's taxonomy level. */
    @Enumerated(EnumType.STRING)
    private BloomLevel bloomLevel = BloomLevel.APPLY;

    /**
     * PO mappings encoded as "PO1:3,PO3:2,PO5:1" where the number is the weight (1-3).
     * Mirrors the standard OBE matrix notation used by BAETE/NBA accreditation.
     */
    @Column(length = 500)
    private String poMappings;

    /** Minimum attainment % to consider the CO met. Default 60% per standard practice. */
    private Integer targetAttainment = 60;

    public enum BloomLevel { REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE }

    public CourseOutcome() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public BloomLevel getBloomLevel() { return bloomLevel; }
    public void setBloomLevel(BloomLevel bloomLevel) { this.bloomLevel = bloomLevel; }
    public String getPoMappings() { return poMappings; }
    public void setPoMappings(String poMappings) { this.poMappings = poMappings; }
    public Integer getTargetAttainment() { return targetAttainment; }
    public void setTargetAttainment(Integer targetAttainment) { this.targetAttainment = targetAttainment; }
}
