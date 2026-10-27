package com.weg.benefits.catalog;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "benefit_discount")
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 15)
    private String value;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    protected Discount() { }

    Discount(String value, String description) {
        this.value = value;
        this.description = description;
    }

    void assignTo(Benefit value) { benefit = value; }

    public UUID getId() { return id; }
    public String getValue() { return value; }
    public String getDescription() { return description; }
}
