package com.wafabureau.gestion.dto.partner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerWriteRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 30) String ice,
        @Size(max = 180) String contactPerson,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 500) String address
) {

    public PartnerWriteRequest {
        name = trim(name);
        ice = trim(ice);
        contactPerson = trim(contactPerson);
        email = trim(email);
        phone = trim(phone);
        address = trim(address);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
