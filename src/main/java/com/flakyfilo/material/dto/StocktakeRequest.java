package com.flakyfilo.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StocktakeRequest(
        @NotNull @PositiveOrZero BigDecimal actualStock,
        String reason,
        LocalDate transactionDate,
        boolean isPeriodic // true=정기 실사, false=수시 보정 (사용자가 모달에서 직접 선택)
) {
}