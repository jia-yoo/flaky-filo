package com.flakyfilo.material.dto;

import com.flakyfilo.common.enums.MaterialUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MaterialUpdateRequest(
        @NotNull String name,
        @NotNull MaterialUnit unit,
        @NotNull @PositiveOrZero BigDecimal minStockThreshold,
        @Positive BigDecimal referenceQuantity,
        @PositiveOrZero BigDecimal referencePrice,
        Long supplierId,
        String note
) {
}