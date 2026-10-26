package com.weg.benefits.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

    @Column(nullable = false, length = 300)
    private String conditions;

    @Column(nullable = false)
    private boolean active;

    @OneToOne(mappedBy = "benefit", fetch = FetchType.LAZY)
    private Establishment establishment;

    @OneToMany(mappedBy = "benefit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Discount> discounts = new ArrayList<>();

    @Column(name = "first_linked_at")
    private Instant firstLinkedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Benefit() { }

    Benefit(String audience, String participationInstructions, String contact,
            String conditions, List<Discount> discounts) {
        this.audience = audience;
        this.participationInstructions = participationInstructions;
        this.contact = contact;
        this.conditions = conditions;
        replaceDiscounts(discounts);
    }

    void update(String audience, String participationInstructions, String contact,
            String conditions, List<Discount> discounts, Boolean active) {
        if (audience != null) this.audience = audience;
        if (participationInstructions != null) this.participationInstructions = participationInstructions;
        if (contact != null) this.contact = contact;
        if (conditions != null) this.conditions = conditions;
        if (discounts != null) replaceDiscounts(discounts);
        if (active != null) this.active = active;
    }

    void linkTo(Establishment value) {
        if (establishment != null && establishment != value) establishment.setBenefit(null);
        establishment = value;
        value.setBenefit(this);
        if (firstLinkedAt == null) {
            firstLinkedAt = Instant.now();
            active = true;
        }
    }

    void unlinkFrom(Establishment value) {
        if (establishment == value) {
            establishment = null;
            value.setBenefit(null);
        }
    }

    private void replaceDiscounts(List<Discount> values) {
        discounts.clear();
        for (Discount value : values) {
            value.assignTo(this);
            discounts.add(value);
        }
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
    public String getConditions() { return conditions; }
    public boolean isActive() { return active; }
    public Establishment getEstablishment() { return establishment; }
    public List<Discount> getDiscounts() { return List.copyOf(discounts); }
    public Instant getFirstLinkedAt() { return firstLinkedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
