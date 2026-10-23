package com.weg.benefits.engagement;

import java.time.Instant;
import java.util.UUID;

import com.weg.benefits.catalog.Establishment;
import com.weg.benefits.identity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_visit",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_visit_event_key",
                columnNames = {"user_id", "establishment_id", "occurred_at"}),
        indexes = @Index(name = "ix_user_visit_owner_time", columnList = "user_id, occurred_at DESC"))
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected Visit() { }

    public Visit(AppUser user, Establishment establishment, Instant occurredAt) {
        this.user = user;
        this.establishment = establishment;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    void onCreate() {
        receivedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public Establishment getEstablishment() { return establishment; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
}
