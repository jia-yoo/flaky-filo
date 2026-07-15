package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReserveStockRequest(
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate closingDate
) {
}