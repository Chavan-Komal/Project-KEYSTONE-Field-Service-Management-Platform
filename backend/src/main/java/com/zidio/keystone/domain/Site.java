package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    // Geocoded (best-effort) from address when the site is created. Null
    // means the lookup failed or hasn't run — map/nearest-technician
    // features simply skip a site with no coordinates.
    private Double latitude;
    private Double longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Site() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Site s = new Site();
        public Builder customer(Customer customer) { s.customer = customer; return this; }
        public Builder name(String name) { s.name = name; return this; }
        public Builder address(String address) { s.address = address; return this; }
        public Builder latitude(Double latitude) { s.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { s.longitude = longitude; return this; }
        public Site build() { return s; }
    }
}
