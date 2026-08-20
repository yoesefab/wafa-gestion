package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier extends PartnerEntity {

    protected Supplier() {
    }

    public Supplier(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address
    ) {
        super(name, ice, contactPerson, email, phone, address);
    }
}
