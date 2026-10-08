package com.flakyfilo.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubstitutionLineRequest(
        @NotNull Long substituteProductId,
        @NotNull @Positive Integer quantity
) {
}
