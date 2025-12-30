package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilityNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FacilityNotificationSettingsRepository extends JpaRepository<FacilityNotificationSettings, UUID> {
}
