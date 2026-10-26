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
@Table(name = "user_favorite",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_favorite_owner_establishment",
                columnNames = {"user_id", "establishment_id"}),
        indexes = @Index(name = "ix_user_favorite_created", columnList = "user_id, created_at"))
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() { }

    public Favorite(AppUser user, Establishment establishment) {
        this.user = user;
        this.establishment = establishment;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public Establishment getEstablishment() { return establishment; }
    public Instant getCreatedAt() { return createdAt; }
}
