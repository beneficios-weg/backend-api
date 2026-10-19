package com.weg.benefits.identity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String normalized = IdentityService.normalizeUsername(login);
        AppUser user = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(normalized, normalized)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Conta inválida"));
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole());
    }

    public record AppUserPrincipal(Integer id, String username, String password, Role role)
            implements UserDetails, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
                getAuthorities() {
            return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_" + role.name()));
        }
        @Override public String getPassword() { return password; }
        @Override public String getUsername() { return username; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    }
}
