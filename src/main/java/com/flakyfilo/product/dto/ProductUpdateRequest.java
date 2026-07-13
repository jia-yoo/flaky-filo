package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank String name,
        String category,
        @PositiveOrZero int price,
        @NotNull @Positive Integer yieldCount,
        @NotNull BigDecimal overheadRate,
        @NotNull BigDecimal targetCostRatio
) {
}
