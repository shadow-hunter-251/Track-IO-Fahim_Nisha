package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.SkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillMappingRepository extends JpaRepository<SkillMapping, Long> {
    List<SkillMapping> findBySkillId(Long skillId);
}
