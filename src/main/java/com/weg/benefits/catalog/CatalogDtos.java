package com.weg.benefits.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public final class CatalogDtos {
    private CatalogDtos() { }

    public record CategoryCreateRequest(
            @NotBlank @Size(min = 6, max = 75) String name,
            @NotBlank @Size(max = 160) @Pattern(regexp = "[a-z0-9-]+:[a-z0-9-]+") String iconKey,
            @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorHex) { }

    public record CategoryBatchCreateRequest(
            @NotEmpty @Size(max = 100) List<@NotNull @Valid CategoryCreateRequest> categories) { }

    public record CategoryPatchRequest(
            @Size(min = 6, max = 75) String name,
            @Size(max = 160) @Pattern(regexp = "[a-z0-9-]+:[a-z0-9-]+") String iconKey,
            @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorHex,
            Boolean active) {
        boolean hasChanges() { return name != null || iconKey != null || colorHex != null || active != null; }
    }

    public record ServiceCreateRequest(@NotBlank @Size(min = 3, max = 75) String name) { }

    public record ServiceBatchCreateRequest(
            @NotEmpty @Size(max = 100) List<@NotNull @Valid ServiceCreateRequest> services) { }

    public record ServicePatchRequest(@Size(min = 3, max = 75) String name, Boolean active) {
        boolean hasChanges() { return name != null || active != null; }
    }

    public record EstablishmentCreateRequest(
            @NotBlank @Size(min = 2) String name,
            @NotBlank String description,
            @Pattern(regexp = "[0-9]{8}") String postalCode,
            @PositiveOrZero Integer streetNumber,
            @Size(min = 3, max = 75) String street,
            @Size(min = 3, max = 75) String district,
            @Size(min = 3, max = 75) String city,
            @Pattern(regexp = "[A-Za-z]{2}") String state,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            BigDecimal altitude,
            @Size(max = 2048) String facadeImageUrl,
            @NotNull UUID categoryId,
            @NotNull UUID benefitId,
            List<@NotNull UUID> serviceIds,
            EstablishmentChannel channel) { }

    public record EstablishmentBatchCreateRequest(
            @NotEmpty @Size(max = 100) List<@NotNull @Valid EstablishmentCreateRequest> establishments) { }

    public record EstablishmentPatchRequest(
            @Size(min = 2) String name,
            String description,
            @Pattern(regexp = "[0-9]{8}") String postalCode,
            @PositiveOrZero Integer streetNumber,
            @Size(min = 3, max = 75) String street,
            @Size(min = 3, max = 75) String district,
            @Size(min = 3, max = 75) String city,
            @Pattern(regexp = "[A-Za-z]{2}") String state,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            BigDecimal altitude,
            @Size(max = 2048) String facadeImageUrl,
            Boolean clearFacadeImageUrl,
            UUID categoryId,
            UUID benefitId,
            List<@NotNull UUID> serviceIds,
            Boolean active,
            EstablishmentChannel channel) {
        boolean hasChanges() {
            return name != null || description != null || postalCode != null || streetNumber != null
                    || street != null || district != null || city != null || state != null
                    || latitude != null || longitude != null || altitude != null
                    || facadeImageUrl != null || Boolean.TRUE.equals(clearFacadeImageUrl)
                    || categoryId != null || benefitId != null || serviceIds != null || active != null || channel != null;
        }
    }

    public record DiscountInput(
            @NotBlank @Size(min = 2, max = 15) String value,
            @NotBlank String description) { }

    public record DiscountResponse(UUID id, String value, String description) {
        static DiscountResponse from(Discount value) {
            return new DiscountResponse(value.getId(), value.getValue(), value.getDescription());
        }
    }

    public record BenefitCreateRequest(
            @NotBlank String audience,
            @NotBlank String participationInstructions,
            @NotBlank String contact,
            @NotEmpty List<@NotNull @Valid DiscountInput> discounts) { }

    public record BenefitBatchCreateRequest(
            @NotEmpty @Size(max = 100) List<@NotNull @Valid BenefitCreateRequest> benefits) { }

    public record BenefitPatchRequest(
            String audience,
            String participationInstructions,
            String contact,
            List<@NotNull @Valid DiscountInput> discounts,
            Boolean active) {
        boolean hasChanges() {
            return audience != null || participationInstructions != null || contact != null
                    || discounts != null || active != null;
        }
    }

    public record CategoryResponse(UUID id, String name, String iconKey, String colorHex,
            boolean active, Instant createdAt, Instant updatedAt) {
        static CategoryResponse from(Category value) {
            return new CategoryResponse(value.getId(), value.getName(), value.getIconKey(), value.getColorHex(),
                    value.isActive(), value.getCreatedAt(), value.getUpdatedAt());
        }
    }

    public record ServiceResponse(UUID id, String name, boolean active, Instant createdAt, Instant updatedAt) {
        static ServiceResponse from(ServiceOption value) {
            return new ServiceResponse(value.getId(), value.getName(), value.isActive(),
                    value.getCreatedAt(), value.getUpdatedAt());
        }
    }

    public record EstablishmentResponse(UUID id, String name, String description, String postalCode,
            Integer streetNumber, String street, String district, String city, String state,
            BigDecimal latitude, BigDecimal longitude, BigDecimal altitude, String facadeImageUrl,
            CategoryResponse category, UUID benefitId, List<ServiceResponse> services,
            EstablishmentChannel channel, boolean mappable, boolean active,
            Instant createdAt, Instant updatedAt) {
        static EstablishmentResponse from(Establishment value) {
            return new EstablishmentResponse(value.getId(), value.getName(), value.getDescription(),
                    value.getPostalCode(), value.getStreetNumber(), value.getStreet(), value.getDistrict(),
                    value.getCity(), value.getState(), value.getLatitude(), value.getLongitude(), value.getAltitude(),
                    value.getFacadeImageUrl(), CategoryResponse.from(value.getCategory()),
                    value.getBenefit() == null ? null : value.getBenefit().getId(),
                    value.getServices().stream().filter(ServiceOption::isActive)
                            .sorted(java.util.Comparator.comparing(ServiceOption::getName))
                            .map(ServiceResponse::from).toList(), value.getChannel(), value.isMappable(),
                    value.isActive(), value.getCreatedAt(),
                    value.getUpdatedAt());
        }
    }

    public record BenefitResponse(UUID id, String audience, String participationInstructions, String contact,
            int establishmentCount, List<DiscountResponse> discounts, boolean active,
            Instant createdAt, Instant updatedAt) {
        static BenefitResponse from(Benefit value) {
            return new BenefitResponse(value.getId(), value.getAudience(), value.getParticipationInstructions(),
                    value.getContact(), value.getEstablishments().size(),
                    value.getDiscounts().stream().map(DiscountResponse::from).toList(), value.isActive(),
                    value.getCreatedAt(), value.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }

    public record CategoryOptionsResponse(List<CategoryPalette.ColorOption> colors, String iconSearchProvider) { }
}
