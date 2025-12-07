package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BagLabelRepository extends JpaRepository<BagLabel, UUID> {
    Optional<BagLabel> findByQrCode(String qrCode);
}
