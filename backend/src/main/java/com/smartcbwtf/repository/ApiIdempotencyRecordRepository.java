package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ApiIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiIdempotencyRecordRepository extends JpaRepository<ApiIdempotencyRecord, UUID> {
    Optional<ApiIdempotencyRecord> findByPrincipalKeyAndIdempotencyScopeAndIdempotencyKey(
            String principalKey,
            String idempotencyScope,
            String idempotencyKey);
}
