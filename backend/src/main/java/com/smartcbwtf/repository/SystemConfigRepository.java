package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findByCategoryOrderByConfigKeyAsc(String category);

    List<SystemConfig> findAllByOrderByCategoryAscConfigKeyAsc();

    boolean existsByConfigKey(String configKey);

    // Find all non-sensitive configs (for mobile app)
    List<SystemConfig> findByIsSensitiveFalseAndCategoryIn(List<String> categories);
}
