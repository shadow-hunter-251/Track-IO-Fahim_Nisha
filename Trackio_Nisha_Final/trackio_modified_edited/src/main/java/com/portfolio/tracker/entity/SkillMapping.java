package com.portfolio.tracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "skill_mappings")
public class SkillMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mappingId;

    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill skill;

    private String courseOutcome;
    private String programOutcome;

    @Enumerated(EnumType.STRING)
    private ProficiencyLevel proficiencyLevel;

    public enum ProficiencyLevel { BASIC, PROFICIENT, EXPERT }

    public SkillMapping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMappingId() { return mappingId; }
    public void setMappingId(String mappingId) { this.mappingId = mappingId; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getCourseOutcome() { return courseOutcome; }
    public void setCourseOutcome(String courseOutcome) { this.courseOutcome = courseOutcome; }
    public String getProgramOutcome() { return programOutcome; }
    public void setProgramOutcome(String programOutcome) { this.programOutcome = programOutcome; }
    public ProficiencyLevel getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SkillMapping sm = new SkillMapping();
        public Builder mappingId(String v) { sm.mappingId = v; return this; }
        public Builder skill(Skill v) { sm.skill = v; return this; }
        public Builder courseOutcome(String v) { sm.courseOutcome = v; return this; }
        public Builder programOutcome(String v) { sm.programOutcome = v; return this; }
        public Builder proficiencyLevel(ProficiencyLevel v) { sm.proficiencyLevel = v; return this; }
        public SkillMapping build() { return sm; }
    }
}
