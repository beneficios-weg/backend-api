package com.weg.benefits.identity;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class IdentityDtos {
    private IdentityDtos() { }

    public record LoginRequest(@NotBlank @Size(max = 254) String login,
            @NotBlank @Size(min = 6, max = 72) String password) { }

    public record CreateUserRequest(@NotBlank @Size(min = 3, max = 75)
            @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            Role role) { }

    public record UpdateUserRequest(@Size(min = 3, max = 75)
            @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
            @Email @Size(max = 254) String email,
            Role role,
            String password) {
        public boolean hasChanges() {
            return username != null || email != null || role != null || password != null;
        }
    }

    public record UserResponse(Integer id, String username, String email, Role role,
            boolean active, Instant createdAt, Instant updatedAt) {
        static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                    user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    public record UserPageResponse(List<UserResponse> content, int page, int size,
            long totalElements, int totalPages) { }
}
