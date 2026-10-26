package com.weg.benefits.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.weg.benefits.catalog.CatalogService;
import com.weg.benefits.catalog.Establishment;
import com.weg.benefits.identity.AppUser;
import com.weg.benefits.identity.IdentityException;
import com.weg.benefits.identity.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class EngagementServiceTests {
    private static final Integer USER_ID = 42;
    private static final UUID ESTABLISHMENT_ID = UUID.fromString("cd7b1d5a-2f57-41b5-998b-8c8d64aa9801");

    private final FavoriteRepository favorites = mock(FavoriteRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final IdentityService identity = mock(IdentityService.class);
    private final CatalogService catalog = mock(CatalogService.class);

    private EngagementService service;

    @BeforeEach
    void setUp() {
        service = new EngagementService(favorites, visits, identity, catalog);
    }

    @Test
    void addingFavoriteUsesAuthenticatedOwnerAndDoesNotDuplicateIt() {
        AppUser owner = mock(AppUser.class);
        Establishment establishment = mock(Establishment.class);
        when(identity.lockActiveUserForEngagement(USER_ID)).thenReturn(owner);
        when(catalog.requireActiveEstablishmentForEngagement(ESTABLISHMENT_ID)).thenReturn(establishment);
        when(favorites.existsByUser_IdAndEstablishment_Id(USER_ID, ESTABLISHMENT_ID))
                .thenReturn(false, true);

        service.addFavorite(USER_ID, ESTABLISHMENT_ID);
        service.addFavorite(USER_ID, ESTABLISHMENT_ID);

        ArgumentCaptor<Favorite> saved = ArgumentCaptor.forClass(Favorite.class);
        verify(favorites).save(saved.capture());
        assertSame(owner, saved.getValue().getUser());
        assertSame(establishment, saved.getValue().getEstablishment());
        verify(favorites, org.mockito.Mockito.times(2))
                .existsByUser_IdAndEstablishment_Id(USER_ID, ESTABLISHMENT_ID);
    }

    @Test
    void inactiveAccountCannotWriteEngagementData() {
        when(identity.lockActiveUserForEngagement(USER_ID))
                .thenThrow(new IdentityException(HttpStatus.UNAUTHORIZED, "Conta indisponível", "Conta inativa."));

        IdentityException exception = assertThrows(IdentityException.class,
                () -> service.removeFavorite(USER_ID, ESTABLISHMENT_ID));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verifyNoInteractions(favorites, catalog, visits);
    }

    @Test
    void newVisitIsSavedWithAuthenticatedOwnerAndOccurrenceTime() {
        AppUser owner = mock(AppUser.class);
        Establishment establishment = mock(Establishment.class);
        Instant occurredAt = Instant.parse("2026-10-23T14:00:00Z");
        when(identity.lockActiveUserForEngagement(USER_ID)).thenReturn(owner);
        when(catalog.requireExistingEstablishmentForEngagement(ESTABLISHMENT_ID)).thenReturn(establishment);
        when(establishment.getId()).thenReturn(ESTABLISHMENT_ID);
        when(visits.findFirstByUser_IdAndEstablishment_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
                USER_ID, ESTABLISHMENT_ID, occurredAt.minusSeconds(1800), occurredAt.plusSeconds(1800)))
                .thenReturn(Optional.empty());
        when(visits.saveAndFlush(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EngagementService.VisitSubmission result = service.recordVisit(USER_ID,
                new EngagementDtos.VisitCreateRequest(ESTABLISHMENT_ID, occurredAt));

        assertTrue(result.created());
        assertEquals(ESTABLISHMENT_ID, result.response().establishmentId());
        assertEquals(occurredAt, result.response().occurredAt());
        ArgumentCaptor<Visit> saved = ArgumentCaptor.forClass(Visit.class);
        verify(visits).saveAndFlush(saved.capture());
        assertSame(owner, saved.getValue().getUser());
        assertSame(establishment, saved.getValue().getEstablishment());
        assertEquals(occurredAt, saved.getValue().getOccurredAt());
    }

    @Test
    void repeatedVisitWithinThirtyMinutesReturnsExistingEventWithoutSaving() {
        AppUser owner = mock(AppUser.class);
        Establishment establishment = mock(Establishment.class);
        Visit existing = mock(Visit.class);
        Instant occurredAt = Instant.parse("2026-10-23T14:00:00Z");
        when(identity.lockActiveUserForEngagement(USER_ID)).thenReturn(owner);
        when(catalog.requireExistingEstablishmentForEngagement(ESTABLISHMENT_ID)).thenReturn(establishment);
        when(establishment.getId()).thenReturn(ESTABLISHMENT_ID);
        when(visits.findFirstByUser_IdAndEstablishment_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
                USER_ID, ESTABLISHMENT_ID, occurredAt.minusSeconds(1800), occurredAt.plusSeconds(1800)))
                .thenReturn(Optional.of(existing));
        when(existing.getId()).thenReturn(UUID.fromString("1e08e3b3-4d87-4360-a855-37bb04f39ab5"));
        when(existing.getEstablishment()).thenReturn(establishment);
        when(existing.getOccurredAt()).thenReturn(occurredAt.minusSeconds(60));

        EngagementService.VisitSubmission result = service.recordVisit(USER_ID,
                new EngagementDtos.VisitCreateRequest(ESTABLISHMENT_ID, occurredAt));

        assertFalse(result.created());
        assertEquals(existing.getId(), result.response().id());
        assertEquals(occurredAt.minusSeconds(60), result.response().occurredAt());
        verify(visits, never()).saveAndFlush(any(Visit.class));
        verify(visits).findFirstByUser_IdAndEstablishment_IdAndOccurredAtBetweenOrderByOccurredAtAsc(
                USER_ID, ESTABLISHMENT_ID, occurredAt.minusSeconds(1800), occurredAt.plusSeconds(1800));
    }
}
