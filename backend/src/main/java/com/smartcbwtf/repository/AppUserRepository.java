package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
        Optional<AppUser> findByUsername(String username);

        int countByFacilityIdAndActive(UUID facilityId, boolean active);

        // Global queries for SuperAdmin user management
        Page<AppUser> findByFacilityId(UUID facilityId, Pageable pageable);

        Page<AppUser> findByRole(String role, Pageable pageable);

        Page<AppUser> findByActive(boolean active, Pageable pageable);

        Page<AppUser> findByFacilityIdAndRole(UUID facilityId, String role, Pageable pageable);

        // Non-pageable version for finding admins
        java.util.List<AppUser> findByFacilityIdAndRole(UUID facilityId, String role);

        Page<AppUser> findByFacilityIdAndActive(UUID facilityId, boolean active, Pageable pageable);

        Page<AppUser> findByRoleAndActive(String role, boolean active, Pageable pageable);

        Page<AppUser> findByFacilityIdAndRoleAndActive(UUID facilityId, String role, boolean active, Pageable pageable);

        @Query("SELECT u FROM AppUser u WHERE " +
                        "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<AppUser> searchUsers(@Param("search") String search, Pageable pageable);

        @Query("SELECT u FROM AppUser u WHERE u.facility.id = :facilityId AND " +
                        "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<AppUser> searchUsersByFacility(@Param("facilityId") UUID facilityId, @Param("search") String search,
                        Pageable pageable);

        boolean existsByUsername(String username);
}
