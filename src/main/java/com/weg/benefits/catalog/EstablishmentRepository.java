package com.weg.benefits.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstablishmentRepository extends JpaRepository<Establishment, java.util.UUID> {
    Page<Establishment> findAllByOrderByIdAsc(Pageable pageable);

    @Query("""
            select distinct e from Establishment e
            where e.active = true and e.category.active = true
              and (:channel is null or e.channel = :channel)
              and (:forMap = false or (e.channel <> com.weg.benefits.catalog.EstablishmentChannel.ONLINE
                   and e.latitude is not null and e.longitude is not null))
              and (:categoryId is null or e.category.id = :categoryId)
              and (:serviceId is null or exists (
                  select s.id from Establishment linked join linked.services s
                  where linked.id = e.id and s.id = :serviceId and s.active = true))
              and (:query = '' or lower(e.name) like concat('%', lower(:query), '%')
                   or lower(e.description) like concat('%', lower(:query), '%'))
            """)
    Page<Establishment> searchActive(@Param("query") String query,
            @Param("categoryId") java.util.UUID categoryId,
            @Param("serviceId") java.util.UUID serviceId,
            @Param("channel") EstablishmentChannel channel, @Param("forMap") boolean forMap,
            Pageable pageable);

    @Query("select e from Establishment e where e.id = :id and e.active = true and e.category.active = true")
    java.util.Optional<Establishment> findActiveById(@Param("id") java.util.UUID id);
}
