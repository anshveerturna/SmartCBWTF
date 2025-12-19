package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);

    int countByFacilityIdAndActive(UUID facilityId, boolean active);
}
