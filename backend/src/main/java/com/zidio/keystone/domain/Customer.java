package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_email", nullable = false, length = 200)
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Customer() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Customer c = new Customer();
        public Builder name(String name) { c.name = name; return this; }
        public Builder contactEmail(String contactEmail) { c.contactEmail = contactEmail; return this; }
        public Customer build() { return c; }
    }
}
