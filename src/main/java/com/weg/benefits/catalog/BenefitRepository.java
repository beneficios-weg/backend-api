package com.weg.benefits.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitRepository extends JpaRepository<Benefit, java.util.UUID> {
    Page<Benefit> findAllByOrderByIdAsc(Pageable pageable);
    Page<Benefit> findAllByActiveTrueAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrueOrderByIdAsc(Pageable pageable);
    Page<Benefit> findAllByActiveTrueAndEstablishment_IdAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrueOrderByIdAsc(
            java.util.UUID establishmentId, Pageable pageable);
}
