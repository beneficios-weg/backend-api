package com.weg.benefits.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefit")
public class Benefit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "audience", nullable = false, columnDefinition = "text")
    private String audience;

    @Column(name = "participation_instructions", nullable = false, length = 300)
    private String participationInstructions;

    @Column(nullable = false, columnDefinition = "text")
    private String contact;

    @Column(nullable = false, columnDefinition = "text")
    private String discount;

    @Column(nullable = false, length = 300)
    private String conditions;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Benefit() { }

    Benefit(String audience, String participationInstructions, String contact,
            String discount, String conditions, Establishment establishment) {
        this.audience = audience;
        this.participationInstructions = participationInstructions;
        this.contact = contact;
        this.discount = discount;
        this.conditions = conditions;
        this.establishment = establishment;
    }

    void update(String audience, String participationInstructions, String contact,
            String discount, String conditions, Establishment establishment, Boolean active) {
        if (audience != null) this.audience = audience;
        if (participationInstructions != null) this.participationInstructions = participationInstructions;
        if (contact != null) this.contact = contact;
        if (discount != null) this.discount = discount;
        if (conditions != null) this.conditions = conditions;
        if (establishment != null) this.establishment = establishment;
        if (active != null) this.active = active;
    }

    void deactivate() { active = false; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getAudience() { return audience; }
    public String getParticipationInstructions() { return participationInstructions; }
    public String getContact() { return contact; }
    public String getDiscount() { return discount; }
    public String getConditions() { return conditions; }
    public boolean isActive() { return active; }
    public Establishment getEstablishment() { return establishment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
