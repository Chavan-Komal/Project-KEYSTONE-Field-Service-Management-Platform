package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A short-lived, single-use token for the self-service password reset flow.
 * Issued by AuthService.requestPasswordReset, consumed by AuthService.resetPassword.
 * Rows are never deleted — a used/expired token stays as an audit record.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PasswordResetToken() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final PasswordResetToken t = new PasswordResetToken();
        public Builder user(User user) { t.user = user; return this; }
        public Builder token(String token) { t.token = token; return this; }
        public Builder expiresAt(Instant expiresAt) { t.expiresAt = expiresAt; return this; }
        public Builder used(boolean used) { t.used = used; return this; }
        public PasswordResetToken build() { return t; }
    }
}
