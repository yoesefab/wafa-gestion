package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.customer.CustomerResponse;
import com.wafabureau.gestion.model.Customer;

public final class CustomerMapper {
    private CustomerMapper() { }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(), customer.getName(), customer.getIce(), customer.getContactPerson(),
                customer.getEmail(), customer.getPhone(), customer.getAddress(), customer.isActive(),
                customer.getDeactivatedAt(), customer.getCreatedAt(), customer.getUpdatedAt(), customer.getVersion()
        );
    }
}
