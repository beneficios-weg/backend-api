package com.weg.benefits.catalog;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogDtos.BenefitBatchCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.BenefitResponse;
import com.weg.benefits.catalog.CatalogDtos.CategoryCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.CategoryPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.CategoryResponse;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentPatchRequest;
import com.weg.benefits.catalog.CatalogDtos.EstablishmentResponse;
import com.weg.benefits.catalog.CatalogDtos.PageResponse;
import com.weg.benefits.catalog.CatalogDtos.ServiceCreateRequest;
import com.weg.benefits.catalog.CatalogDtos.ServicePatchRequest;
import com.weg.benefits.catalog.CatalogDtos.ServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
public class CatalogAdminController {
    private final CatalogService catalog;

    public CatalogAdminController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/categories")
    public PageResponse<CategoryResponse> categories(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.adminCategories(page, size); }

    @GetMapping("/categories/{id}")
    public CategoryResponse category(@PathVariable UUID id) { return catalog.getCategory(id); }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        var created = catalog.createCategory(request);
        return ResponseEntity.created(URI.create("/v1/admin/categories/" + created.id())).body(created);
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryPatchRequest request) {
        return catalog.updateCategory(id, request);
    }

    @PostMapping("/categories/{id}/deactivation")
    public ResponseEntity<Void> deactivateCategory(@PathVariable UUID id) {
        catalog.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/services")
    public PageResponse<ServiceResponse> services(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.adminServices(page, size); }

    @GetMapping("/services/{id}")
    public ServiceResponse service(@PathVariable UUID id) { return catalog.getService(id); }

    @PostMapping("/services")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceCreateRequest request) {
        var created = catalog.createService(request);
        return ResponseEntity.created(URI.create("/v1/admin/services/" + created.id())).body(created);
    }

    @PatchMapping("/services/{id}")
    public ServiceResponse updateService(@PathVariable UUID id, @Valid @RequestBody ServicePatchRequest request) {
        return catalog.updateService(id, request);
    }

    @PostMapping("/services/{id}/deactivation")
    public ResponseEntity<Void> deactivateService(@PathVariable UUID id) {
        catalog.deactivateService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/establishments")
    public PageResponse<EstablishmentResponse> establishments(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.adminEstablishments(page, size); }

    @GetMapping("/establishments/{id}")
    public EstablishmentResponse establishment(@PathVariable UUID id) {
        return catalog.getAdminEstablishment(id);
    }

    @PostMapping("/establishments")
    public ResponseEntity<EstablishmentResponse> createEstablishment(
            @Valid @RequestBody EstablishmentCreateRequest request) {
        var created = catalog.createEstablishment(request);
        return ResponseEntity.created(URI.create("/v1/admin/establishments/" + created.id())).body(created);
    }

    @PatchMapping("/establishments/{id}")
    public EstablishmentResponse updateEstablishment(@PathVariable UUID id,
            @Valid @RequestBody EstablishmentPatchRequest request) {
        return catalog.updateEstablishment(id, request);
    }

    @PostMapping("/establishments/{id}/deactivation")
    public ResponseEntity<Void> deactivateEstablishment(@PathVariable UUID id) {
        catalog.deactivateEstablishment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/benefits")
    public PageResponse<BenefitResponse> benefits(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return catalog.adminBenefits(page, size); }

    @GetMapping("/benefits/{id}")
    public BenefitResponse benefit(@PathVariable UUID id) { return catalog.getBenefit(id); }

    @PostMapping("/benefits")
    public ResponseEntity<BenefitResponse> createBenefit(@Valid @RequestBody BenefitCreateRequest request) {
        var created = catalog.createBenefit(request);
        return ResponseEntity.created(URI.create("/v1/admin/benefits/" + created.id())).body(created);
    }

    @PostMapping("/benefits/batch")
    public ResponseEntity<List<BenefitResponse>> createBenefits(
            @Valid @RequestBody BenefitBatchCreateRequest request) {
        return ResponseEntity.status(201).body(catalog.createBenefits(request));
    }

    @PatchMapping("/benefits/{id}")
    public BenefitResponse updateBenefit(@PathVariable UUID id, @Valid @RequestBody BenefitPatchRequest request) {
        return catalog.updateBenefit(id, request);
    }

    @PostMapping("/benefits/{id}/deactivation")
    public ResponseEntity<Void> deactivateBenefit(@PathVariable UUID id) {
        catalog.deactivateBenefit(id);
        return ResponseEntity.noContent().build();
    }
}
