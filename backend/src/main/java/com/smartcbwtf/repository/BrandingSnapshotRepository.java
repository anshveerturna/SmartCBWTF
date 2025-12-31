package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BrandingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BrandingSnapshotRepository extends JpaRepository<BrandingSnapshot, UUID> {
}
