package com.weg.benefits.engagement;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    @EntityGraph(attributePaths = {"establishment", "establishment.category"})
    Page<Favorite> findAllByUser_IdAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrue(
            Integer userId, Pageable pageable);

    boolean existsByUser_IdAndEstablishment_Id(Integer userId, UUID establishmentId);

    long deleteByUser_IdAndEstablishment_Id(Integer userId, UUID establishmentId);
}
