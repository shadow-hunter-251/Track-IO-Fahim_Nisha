package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.ProgramOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgramOutcomeRepository extends JpaRepository<ProgramOutcome, Long> {
    Optional<ProgramOutcome> findByCode(String code);
    List<ProgramOutcome> findAllByOrderByCodeAsc();
}
