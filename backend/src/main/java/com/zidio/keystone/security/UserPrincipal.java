package com.zidio.keystone.security;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapts our User entity to Spring Security. Also carries userId / customerId /
 * role in an easily-reachable form so services can enforce ownership checks
 * without another database round trip.
 */
public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final UUID customerId; // only set for CUSTOMER-role users
    private final String name;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        this.name = user.getName();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
