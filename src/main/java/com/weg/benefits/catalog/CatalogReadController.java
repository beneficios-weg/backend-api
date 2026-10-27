package com.weg.benefits.catalog;

import com.weg.benefits.catalog.CatalogDtos.BenefitResponse;
import com.weg.benefits.catalog.CatalogDtos.CategoryOptionsResponse;
import com.weg.benefits.catalog.CatalogDtos.CategoryResponse;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentResponse;
import com.weg.benefits.catalog.CatalogDtos.PageResponse;
import com.weg.benefits.catalog.CatalogDtos.ServiceResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class CatalogReadController {
    private final CatalogService catalog;

    public CatalogReadController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/categories/options")
    public CategoryOptionsResponse categoryOptions() { return catalog.categoryOptions(); }

    @GetMapping("/categories")
    public PageResponse<CategoryResponse> categories(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.activeCategories(page, size); }

    @GetMapping("/services")
    public PageResponse<ServiceResponse> services(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.activeServices(page, size); }

    @GetMapping("/establishments")
    public PageResponse<EstablishmentResponse> establishments(@RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) EstablishmentChannel channel,
            @RequestParam(defaultValue = "false") boolean forMap,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.searchEstablishments(query, categoryId, serviceId, channel, forMap, page, size);
    }

    @GetMapping("/establishments/{id}")
    public EstablishmentResponse establishment(@PathVariable UUID id) {
        return catalog.getActiveEstablishment(id);
    }

    @GetMapping("/establishments/{id}/benefits")
    public PageResponse<BenefitResponse> benefits(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.activeBenefits(id, page, size);
    }
}
