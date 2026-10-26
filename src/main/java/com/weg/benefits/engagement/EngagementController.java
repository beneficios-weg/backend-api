package com.weg.benefits.engagement;

import java.util.UUID;

import com.weg.benefits.engagement.EngagementDtos.FavoriteResponse;
import com.weg.benefits.engagement.EngagementDtos.PageResponse;
import com.weg.benefits.engagement.EngagementDtos.VisitCreateRequest;
import com.weg.benefits.engagement.EngagementDtos.VisitResponse;
import com.weg.benefits.identity.DatabaseUserDetailsService.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class EngagementController {
    private final EngagementService engagement;

    public EngagementController(EngagementService engagement) {
        this.engagement = engagement;
    }

    @GetMapping("/favorites")
    public PageResponse<FavoriteResponse> favorites(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return engagement.favorites(principal(authentication).id(), page, size);
    }

    @PutMapping("/favorites/{establishmentId}")
    public ResponseEntity<Void> addFavorite(@PathVariable UUID establishmentId, Authentication authentication) {
        engagement.addFavorite(principal(authentication).id(), establishmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorites/{establishmentId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID establishmentId, Authentication authentication) {
        engagement.removeFavorite(principal(authentication).id(), establishmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/visits")
    public PageResponse<VisitResponse> visits(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return engagement.visits(principal(authentication).id(), page, size);
    }

    @PostMapping("/visits")
    public ResponseEntity<VisitResponse> createVisit(@Valid @RequestBody VisitCreateRequest request,
            Authentication authentication) {
        EngagementService.VisitSubmission result = engagement.recordVisit(principal(authentication).id(), request);
        return result.created()
                ? ResponseEntity.status(201).body(result.response())
                : ResponseEntity.ok(result.response());
    }

    private AppUserPrincipal principal(Authentication authentication) {
        return (AppUserPrincipal) authentication.getPrincipal();
    }
}
