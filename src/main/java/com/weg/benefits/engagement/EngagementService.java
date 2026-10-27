package com.weg.benefits.engagement;

import java.time.Duration;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogService;
import com.weg.benefits.catalog.Establishment;
import com.weg.benefits.catalog.EstablishmentChannel;
import com.weg.benefits.identity.AppUser;
import com.weg.benefits.identity.IdentityService;
import com.weg.benefits.engagement.EngagementDtos.FavoriteResponse;
import com.weg.benefits.engagement.EngagementDtos.PageResponse;
import com.weg.benefits.engagement.EngagementDtos.VisitCreateRequest;
import com.weg.benefits.engagement.EngagementDtos.VisitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EngagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(30);

    private final FavoriteRepository favorites;
    private final VisitRepository visits;
    private final IdentityService identity;
    private final CatalogService catalog;

    public EngagementService(FavoriteRepository favorites, VisitRepository visits,
            IdentityService identity, CatalogService catalog) {
        this.favorites = favorites;
        this.visits = visits;
        this.identity = identity;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriteResponse> favorites(Integer userId, int page, int requestedSize) {
        Page<Favorite> result = favorites.findAllByUser_IdAndEstablishment_ActiveTrueAndEstablishment_Category_ActiveTrue(
                userId, pageRequest(page, requestedSize, Sort.by(Sort.Order.desc("createdAt"),
                        Sort.Order.asc("establishment.id"))));
        return PageResponse.from(result, FavoriteResponse::from);
    }

    @Transactional
    public void addFavorite(Integer userId, UUID establishmentId) {
        AppUser owner = identity.lockActiveUserForEngagement(userId);
        Establishment establishment = catalog.requireActiveEstablishmentForEngagement(establishmentId);
        if (!favorites.existsByUser_IdAndEstablishment_Id(userId, establishmentId)) {
            favorites.save(new Favorite(owner, establishment));
        }
    }

    @Transactional
    public void removeFavorite(Integer userId, UUID establishmentId) {
        identity.lockActiveUserForEngagement(userId);
        favorites.deleteByUser_IdAndEstablishment_Id(userId, establishmentId);
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> visits(Integer userId, int page, int requestedSize) {
        Page<Visit> result = visits.findAllByUser_Id(userId,
                pageRequest(page, requestedSize, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return PageResponse.from(result, VisitResponse::from);
    }

    @Transactional
    public VisitSubmission recordVisit(Integer userId, VisitCreateRequest request) {
        AppUser owner = identity.lockActiveUserForEngagement(userId);
        Establishment establishment = catalog.requireExistingEstablishmentForEngagement(request.establishmentId());
        if (establishment.getChannel() == EstablishmentChannel.ONLINE) {
            throw new EngagementException(HttpStatus.BAD_REQUEST, "Visita indisponível",
                    "Parceiro exclusivamente online não possui local físico para registrar visita.");
        }
        Instant occurredAt = request.occurredAt();
        Instant from;
        Instant to;
        try {
            from = occurredAt.minus(DUPLICATE_WINDOW);
            to = occurredAt.plus(DUPLICATE_WINDOW);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new EngagementException(HttpStatus.BAD_REQUEST, "Data inválida",
                    "Informe uma data de ocorrência válida.");
        }
        var existing = visits.findFirstByUser_IdAndEstablishment_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
                userId, establishment.getId(), from, to);
        if (existing.isPresent()) {
            return new VisitSubmission(VisitResponse.from(existing.get()), false);
        }

        Visit created = visits.saveAndFlush(new Visit(owner, establishment, occurredAt));
        return new VisitSubmission(VisitResponse.from(created), true);
    }

    private PageRequest pageRequest(int page, int requestedSize, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE), sort);
    }

    public record VisitSubmission(VisitResponse response, boolean created) { }
}
