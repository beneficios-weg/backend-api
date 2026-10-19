package com.weg.benefits.identity;

import java.net.URI;
import com.weg.benefits.identity.IdentityDtos.CreateUserRequest;
import com.weg.benefits.identity.IdentityDtos.UpdateUserRequest;
import com.weg.benefits.identity.IdentityDtos.UserResponse;
import com.weg.benefits.identity.IdentityDtos.UserPageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/users")
public class AdminUserController {
    private final IdentityService identityService;

    public AdminUserController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = identityService.createByAdmin(request);
        return ResponseEntity.created(URI.create("/v1/admin/users/" + created.id())).body(created);
    }

    @GetMapping
    public UserPageResponse list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return identityService.listUsers(page, size);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Integer id) {
        return identityService.getUser(id);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        return identityService.updateUser(id, request);
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id, Authentication authentication) {
        var principal = (DatabaseUserDetailsService.AppUserPrincipal) authentication.getPrincipal();
        identityService.deactivateUser(id, principal.id());
        return ResponseEntity.noContent().build();
    }
}
