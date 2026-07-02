package com.smartcbwtf.repository;

import com.smartcbwtf.domain.OAuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OAuthRefreshTokenRepository extends JpaRepository<OAuthRefreshToken, UUID> {
    Optional<OAuthRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE OAuthRefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.client.clientId = :clientId
              AND token.revokedAt IS NULL
            """)
    int revokeActiveTokensForClient(@Param("clientId") String clientId, @Param("revokedAt") Instant revokedAt);
}
