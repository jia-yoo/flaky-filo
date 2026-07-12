package com.flakyfilo.material.dto;

import com.flakyfilo.common.StockTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockAdjustRequest(
        @NotNull StockTransactionType type, // IN 또는 OUT만 (ADJUST는 별도 API)
        @NotNull @Positive BigDecimal quantity,
        String reason,
        LocalDate transactionDate // null이면 Service가 오늘 날짜로 처리
) {
}