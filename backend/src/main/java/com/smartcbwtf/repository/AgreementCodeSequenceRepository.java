package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AgreementCodeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AgreementCodeSequenceRepository extends JpaRepository<AgreementCodeSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AgreementCodeSequence s WHERE s.year = :year")
    Optional<AgreementCodeSequence> findByYearForUpdate(@Param("year") Integer year);
}
