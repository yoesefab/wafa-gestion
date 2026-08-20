package com.wafabureau.gestion.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryWriteRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description
) {
}
