package com.weg.benefits.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOptionRepository extends JpaRepository<ServiceOption, java.util.UUID> {
    Page<ServiceOption> findAllByOrderByIdAsc(Pageable pageable);
    Page<ServiceOption> findAllByActiveTrueOrderByNameAsc(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, java.util.UUID id);
}
