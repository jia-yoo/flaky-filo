package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConversionRecipeItemRequest(
        @NotNull Long materialId,
        @NotNull @Positive BigDecimal perUnitQuantity
) {
}