package com.weg.benefits.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "establishment")
public class Establishment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 75)
    private String name;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "postal_code", nullable = false, length = 8)
    private String postalCode;

    @Column(name = "street_number", nullable = false)
    private Integer streetNumber;

    @Column(nullable = false, length = 75)
    private String street;

    @Column(nullable = false, length = 75)
    private String district;

    @Column(nullable = false, length = 75)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(precision = 9, scale = 2)
    private BigDecimal altitude;

    @Column(name = "facade_image_url", length = 2048)
    private String facadeImageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "establishment_service",
            joinColumns = @JoinColumn(name = "establishment_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceOption> services = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Establishment() { }

    Establishment(String name, String description, String postalCode, Integer streetNumber,
            String street, String district, String city, String state, BigDecimal latitude,
            BigDecimal longitude, BigDecimal altitude, String facadeImageUrl,
            Category category, Set<ServiceOption> services) {
        this.name = name;
        this.description = description;
        this.postalCode = postalCode;
        this.streetNumber = streetNumber;
        this.street = street;
        this.district = district;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.facadeImageUrl = facadeImageUrl;
        this.category = category;
        this.services = new HashSet<>(services);
    }

    void update(String name, String description, String postalCode, Integer streetNumber,
            String street, String district, String city, String state, BigDecimal latitude,
            BigDecimal longitude, BigDecimal altitude, String facadeImageUrl,
            Category category, Set<ServiceOption> services, Boolean active) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (postalCode != null) this.postalCode = postalCode;
        if (streetNumber != null) this.streetNumber = streetNumber;
        if (street != null) this.street = street;
        if (district != null) this.district = district;
        if (city != null) this.city = city;
        if (state != null) this.state = state;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (altitude != null) this.altitude = altitude;
        if (facadeImageUrl != null) this.facadeImageUrl = facadeImageUrl;
        if (category != null) this.category = category;
        if (services != null) this.services = new HashSet<>(services);
        if (active != null) this.active = active;
    }

    void deactivate() { active = false; }

    void clearFacadeImageUrl() { facadeImageUrl = null; }

    void setBenefit(Benefit value) { benefit = value; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPostalCode() { return postalCode; }
    public Integer getStreetNumber() { return streetNumber; }
    public String getStreet() { return street; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getAltitude() { return altitude; }
    public String getFacadeImageUrl() { return facadeImageUrl; }
    public boolean isActive() { return active; }
    public Category getCategory() { return category; }
    public Benefit getBenefit() { return benefit; }
    public Set<ServiceOption> getServices() { return Set.copyOf(services); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
