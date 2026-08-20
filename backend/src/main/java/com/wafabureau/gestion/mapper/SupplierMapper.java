package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.supplier.SupplierResponse;
import com.wafabureau.gestion.model.Supplier;

public final class SupplierMapper {
    private SupplierMapper() { }

    public static SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(), supplier.getName(), supplier.getIce(), supplier.getContactPerson(),
                supplier.getEmail(), supplier.getPhone(), supplier.getAddress(), supplier.isActive(),
                supplier.getDeactivatedAt(), supplier.getCreatedAt(), supplier.getUpdatedAt(), supplier.getVersion()
        );
    }
}
