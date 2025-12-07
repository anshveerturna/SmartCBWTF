package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Hcf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HcfRepository extends JpaRepository<Hcf, UUID> {
    Optional<Hcf> findByCode(String code);
}
