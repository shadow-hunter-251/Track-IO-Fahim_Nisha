package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.SkillEndorsement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillEndorsementRepository extends JpaRepository<SkillEndorsement, Long> {

    /** All endorsements a student has requested (any status). */
    List<SkillEndorsement> findByStudentIdOrderByRequestedAtDesc(Long studentId);

    /** All endorsements requested from a particular teacher (any status). */
    List<SkillEndorsement> findByTeacherUserIdOrderByRequestedAtDesc(Long teacherUserId);

    /** Just the pending queue for a teacher. */
    List<SkillEndorsement> findByTeacherUserIdAndStatusOrderByRequestedAtDesc(
            Long teacherUserId, SkillEndorsement.Status status);

    /** Approved endorsements for a particular skill -- used to render verified badges. */
    List<SkillEndorsement> findBySkillIdAndStatus(Long skillId, SkillEndorsement.Status status);

    /** Has the student already asked this teacher about this skill? */
    boolean existsBySkillIdAndTeacherUserIdAndStatus(
            Long skillId, Long teacherUserId, SkillEndorsement.Status status);

    /** For analytics -- count by status across the whole system. */
    long countByStatus(SkillEndorsement.Status status);
}
