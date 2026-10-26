package com.weg.benefits.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface BenefitRepository extends JpaRepository<Benefit, java.util.UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Benefit b where b.id = :id")
    Optional<Benefit> findByIdForUpdate(@Param("id") UUID id);

    Page<Benefit> findAllByOrderByIdAsc(Pageable pageable);
    Page<Benefit> findAllByActiveTrueAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrueOrderByIdAsc(Pageable pageable);
    Page<Benefit> findAllByActiveTrueAndEstablishment_IdAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrueOrderByIdAsc(
            java.util.UUID establishmentId, Pageable pageable);
}
