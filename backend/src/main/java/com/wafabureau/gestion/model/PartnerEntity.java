package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import com.wafabureau.gestion.model.AuditableEntity;

@MappedSuperclass
public abstract class PartnerEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 30)
    private String ice;

    @Column(name = "contact_person", length = 180)
    private String contactPerson;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected PartnerEntity() {
    }

    protected PartnerEntity(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address
    ) {
        applyDetails(name, ice, contactPerson, email, phone, address);
        this.active = true;
    }

    public void update(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address
    ) {
        applyDetails(name, ice, contactPerson, email, phone, address);
    }

    public void archive() {
        this.active = false;
        this.deactivatedAt = Instant.now();
    }

    private void applyDetails(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address
    ) {
        this.name = name.trim();
        this.ice = normalizeOptional(ice);
        this.contactPerson = normalizeOptional(contactPerson);
        this.email = normalizeEmail(email);
        this.phone = normalizeOptional(phone);
        this.address = normalizeOptional(address);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIce() {
        return ice;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
