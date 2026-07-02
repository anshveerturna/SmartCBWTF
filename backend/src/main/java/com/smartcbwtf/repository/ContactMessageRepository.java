package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant createdAt);

    long countBySourceIpAndCreatedAtAfter(String sourceIp, Instant createdAt);
}
