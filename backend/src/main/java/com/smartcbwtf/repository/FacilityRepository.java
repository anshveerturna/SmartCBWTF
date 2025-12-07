package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    Optional<Facility> findByCode(String code);
}
