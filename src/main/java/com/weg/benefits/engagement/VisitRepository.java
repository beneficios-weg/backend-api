package com.weg.benefits.engagement;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
    @EntityGraph(attributePaths = "establishment")
    Page<Visit> findAllByUser_Id(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = "establishment")
    Optional<Visit> findFirstByUser_IdAndEstablishment_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
            Integer userId, UUID establishmentId, Instant from, Instant to);
}
