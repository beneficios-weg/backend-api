package com.weg.benefits.engagement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

public final class EngagementDtos {
    private EngagementDtos() { }

    public record VisitCreateRequest(
            @NotNull UUID establishmentId,
            @NotNull Instant occurredAt) { }

    public record FavoriteResponse(UUID establishmentId, String name, String city, String state,
            String categoryName, String iconKey, String colorHex, Instant favoritedAt) {
        static FavoriteResponse from(Favorite favorite) {
            var establishment = favorite.getEstablishment();
            var category = establishment.getCategory();
            return new FavoriteResponse(establishment.getId(), establishment.getName(), establishment.getCity(),
                    establishment.getState(), category.getName(), category.getIconKey(), category.getColorHex(),
                    favorite.getCreatedAt());
        }
    }

    public record VisitResponse(UUID id, UUID establishmentId, Instant occurredAt, Instant receivedAt) {
        static VisitResponse from(Visit visit) {
            return new VisitResponse(visit.getId(), visit.getEstablishment().getId(), visit.getOccurredAt(),
                    visit.getReceivedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <E, T> PageResponse<T> from(Page<E> page, java.util.function.Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
