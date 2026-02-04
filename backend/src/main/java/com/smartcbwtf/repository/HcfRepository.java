package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Hcf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.smartcbwtf.domain.DuesClearStatus;

public interface HcfRepository extends JpaRepository<Hcf, UUID> {

    // For monthly reset scheduler
    List<Hcf> findByDuesClearStatusNot(DuesClearStatus status);

    Optional<Hcf> findByCode(String code);

    // ============================================================================
    // LEGACY DUPLICATE DETECTION (global - for backwards compatibility)
    // ============================================================================
    Optional<Hcf> findByPanNo(String panNo);

    Optional<Hcf> findByGstNo(String gstNo);

    Optional<Hcf> findByAadharNo(String aadharNo);

    Optional<Hcf> findByContactPhone(String contactPhone);

    // ============================================================================
    // ENTERPRISE DUPLICATE DETECTION - Agreement Status Aware
    // Only blocks registration if existing HCF has an ACTIVE agreement
    // ============================================================================

    /**
     * Find HCF with matching PAN number that has an ACTIVE agreement.
     * Used to prevent duplicate registrations only when HCF is actively serviced.
     */
    @Query("""
                SELECT h FROM Hcf h
                JOIN Agreement a ON a.hcf = h
                WHERE h.panNo = :panNo
                AND a.status = 'ACTIVE'
            """)
    Optional<Hcf> findByPanNoWithActiveAgreement(@Param("panNo") String panNo);

    /**
     * Find HCF with matching GST number that has an ACTIVE agreement.
     */
    @Query("""
                SELECT h FROM Hcf h
                JOIN Agreement a ON a.hcf = h
                WHERE h.gstNo = :gstNo
                AND a.status = 'ACTIVE'
            """)
    Optional<Hcf> findByGstNoWithActiveAgreement(@Param("gstNo") String gstNo);

    /**
     * Find HCF with matching Aadhar number that has an ACTIVE agreement.
     */
    @Query("""
                SELECT h FROM Hcf h
                JOIN Agreement a ON a.hcf = h
                WHERE h.aadharNo = :aadharNo
                AND a.status = 'ACTIVE'
            """)
    Optional<Hcf> findByAadharNoWithActiveAgreement(@Param("aadharNo") String aadharNo);

    /**
     * Find HCF with matching contact phone that has an ACTIVE agreement.
     */
    @Query("""
                SELECT h FROM Hcf h
                JOIN Agreement a ON a.hcf = h
                WHERE h.contactPhone = :phone
                AND a.status = 'ACTIVE'
            """)
    Optional<Hcf> findByContactPhoneWithActiveAgreement(@Param("phone") String phone);

    /**
     * Find HCF with matching contact email that has an ACTIVE agreement.
     */
    @Query("""
                SELECT h FROM Hcf h
                JOIN Agreement a ON a.hcf = h
                WHERE LOWER(h.contactEmail) = LOWER(:email)
                AND a.status = 'ACTIVE'
            """)
    Optional<Hcf> findByContactEmailWithActiveAgreement(@Param("email") String email);

    // Legacy email lookup
    Optional<Hcf> findByContactEmailIgnoreCase(String email);

    // ============================================================================
    // GPS PROXIMITY DETECTION - Anti-fraud for same location different identity
    // Uses Haversine formula for accurate distance calculation
    // ============================================================================

    /**
     * Find HCFs with ACTIVE agreements within specified radius of given
     * coordinates.
     * Uses Haversine formula: 6371000 meters = Earth's radius in meters
     * 
     * @param lat          Latitude of new registration
     * @param lon          Longitude of new registration
     * @param radiusMeters Detection radius in meters (e.g., 100)
     * @return List of nearby HCFs with active agreements
     */
    @Query(value = """
                SELECT h.* FROM hcf h
                INNER JOIN agreement a ON a.hcf_id = h.id
                WHERE a.status = 'ACTIVE'
                AND h.gps_lat IS NOT NULL
                AND h.gps_lon IS NOT NULL
                AND (
                    6371000 * acos(
                        LEAST(1.0, GREATEST(-1.0,
                            cos(radians(:lat)) * cos(radians(h.gps_lat))
                            * cos(radians(h.gps_lon) - radians(:lon))
                            + sin(radians(:lat)) * sin(radians(h.gps_lat))
                        ))
                    )
                ) < :radiusMeters
                LIMIT 5
            """, nativeQuery = true)
    List<Hcf> findNearbyWithActiveAgreement(
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("radiusMeters") Double radiusMeters);

    /**
     * Calculate distance between two coordinates using Haversine formula.
     * Returns distance in meters. Used for reporting proximity conflicts.
     */
    @Query(value = """
                SELECT (
                    6371000 * acos(
                        LEAST(1.0, GREATEST(-1.0,
                            cos(radians(:lat1)) * cos(radians(:lat2))
                            * cos(radians(:lon2) - radians(:lon1))
                            + sin(radians(:lat1)) * sin(radians(:lat2))
                        ))
                    )
                ) as distance_meters
            """, nativeQuery = true)
    Double calculateDistance(
            @Param("lat1") Double lat1,
            @Param("lon1") Double lon1,
            @Param("lat2") Double lat2,
            @Param("lon2") Double lon2);

    // ============================================================================
    // MASTER DATA QUERIES - SuperAdmin
    // ============================================================================
    Page<Hcf> findByStatus(String status, Pageable pageable);

    @Query("SELECT h FROM Hcf h WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(h.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Hcf> searchByNameOrCode(@Param("search") String search, Pageable pageable);

    // ============================================================================
    // FILTER QUERIES - City, State, HCF Type
    // ============================================================================
    List<Hcf> findByCity(String city);

    List<Hcf> findByState(String state);

    List<Hcf> findByHcfType(com.smartcbwtf.domain.HcfType hcfType);

    @Query("SELECT DISTINCT h.city FROM Hcf h WHERE h.city IS NOT NULL ORDER BY h.city")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT h.state FROM Hcf h WHERE h.state IS NOT NULL ORDER BY h.state")
    List<String> findDistinctStates();
}
