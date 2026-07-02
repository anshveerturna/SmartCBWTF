package com.smartcbwtf.repository;

import com.smartcbwtf.domain.OAuthAuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAuthorizationCodeRepository extends JpaRepository<OAuthAuthorizationCode, UUID> {
    Optional<OAuthAuthorizationCode> findByCodeHash(String codeHash);
}
