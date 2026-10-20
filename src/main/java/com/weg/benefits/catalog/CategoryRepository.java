package com.weg.benefits.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, java.util.UUID> {
    Page<Category> findAllByOrderByIdAsc(Pageable pageable);
    Page<Category> findAllByActiveTrueOrderByNameAsc(Pageable pageable);
}
