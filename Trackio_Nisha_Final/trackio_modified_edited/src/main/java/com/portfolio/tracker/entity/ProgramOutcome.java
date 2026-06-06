package com.portfolio.tracker.entity;

import jakarta.persistence.*;

/**
 * Program Outcome -- a graduate attribute defined per the Washington Accord
 * (PO1..PO12 in Bangladesh BAETE / India NBA / international engineering accreditation).
 *
 * Seeded automatically on first boot by DataInitializer.
 */
@Entity
@Table(name = "program_outcomes")
public class ProgramOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The PO code -- e.g. "PO1", "PO2"... "PO12" */
    @Column(unique = true, nullable = false, length = 10)
    private String code;

    /** Short name -- e.g. "Engineering Knowledge" */
    @Column(nullable = false)
    private String shortName;

    /** Full statement of the outcome (paraphrased from the Washington Accord). */
    @Column(length = 2000)
    private String statement;

    public ProgramOutcome() {}

    public ProgramOutcome(String code, String shortName, String statement) {
        this.code = code;
        this.shortName = shortName;
        this.statement = statement;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
}
