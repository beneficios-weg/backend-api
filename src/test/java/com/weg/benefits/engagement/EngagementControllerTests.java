package com.weg.benefits.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.weg.benefits.identity.DatabaseUserDetailsService.AppUserPrincipal;
import com.weg.benefits.identity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class EngagementControllerTests {
    private static final Integer USER_ID = 42;
    private static final UUID ESTABLISHMENT_ID = UUID.fromString("cd7b1d5a-2f57-41b5-998b-8c8d64aa9801");

    private final EngagementService engagement = mock(EngagementService.class);
    private final EngagementController controller = new EngagementController(engagement);
    private final Authentication authentication = new UsernamePasswordAuthenticationToken(
            new AppUserPrincipal(USER_ID, "collaborator", "not-a-real-password", Role.COLLABORATOR), null);

    @Test
    void favoriteOperationsAlwaysUseTheAuthenticatedAccount() {
        controller.addFavorite(ESTABLISHMENT_ID, authentication);
        controller.removeFavorite(ESTABLISHMENT_ID, authentication);

        verify(engagement).addFavorite(USER_ID, ESTABLISHMENT_ID);
        verify(engagement).removeFavorite(USER_ID, ESTABLISHMENT_ID);
    }

    @Test
    void newAndRepeatedOfflineVisitsReturnCreatedAndOk() {
        Instant occurredAt = Instant.parse("2026-10-23T14:00:00Z");
        EngagementDtos.VisitCreateRequest request = new EngagementDtos.VisitCreateRequest(ESTABLISHMENT_ID,
                occurredAt);
        EngagementDtos.VisitResponse response = new EngagementDtos.VisitResponse(
                UUID.fromString("1e08e3b3-4d87-4360-a855-37bb04f39ab5"), ESTABLISHMENT_ID, occurredAt, occurredAt);
        when(engagement.recordVisit(USER_ID, request))
                .thenReturn(new EngagementService.VisitSubmission(response, true),
                        new EngagementService.VisitSubmission(response, false));

        var created = controller.createVisit(request, authentication);
        var duplicate = controller.createVisit(request, authentication);

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertSame(response, created.getBody());
        assertEquals(HttpStatus.OK, duplicate.getStatusCode());
        assertSame(response, duplicate.getBody());
        verify(engagement, org.mockito.Mockito.times(2)).recordVisit(USER_ID, request);
    }
}
