package com.weg.benefits.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 75)
    private String name;

    @Column(name = "icon_key", nullable = false, length = 160)
    private String iconKey;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Category() { }

    Category(String name, String iconKey, String colorHex) {
        this.name = name;
        this.iconKey = iconKey;
        this.colorHex = colorHex;
    }

    void update(String name, String iconKey, String colorHex, Boolean active) {
        if (name != null) this.name = name;
        if (iconKey != null) this.iconKey = iconKey;
        if (colorHex != null) this.colorHex = colorHex;
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
    public String getName() { return name; }
    public String getIconKey() { return iconKey; }
    public String getColorHex() { return colorHex; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
