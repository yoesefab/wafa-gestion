package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.wafabureau.gestion.model.AuditableEntity;

@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120, unique = true)
    private String normalizedName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected Category() {
    }

    public Category(String name, String description) {
        applyDetails(name, description);
        this.active = true;
    }

    public void update(String name, String description) {
        applyDetails(name, description);
    }

    public void archive() {
        this.active = false;
        this.deactivatedAt = Instant.now();
    }

    private void applyDetails(String name, String description) {
        this.name = name.trim();
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
        this.description = normalizeOptional(description);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
