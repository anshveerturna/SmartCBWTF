package com.smartcbwtf.repository;

import com.smartcbwtf.domain.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthClientRepository extends JpaRepository<OAuthClient, String> {
}
