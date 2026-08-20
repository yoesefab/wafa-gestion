package com.wafabureau.gestion.dto.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ArchiveRequest(@NotNull @PositiveOrZero Long version) {
}
