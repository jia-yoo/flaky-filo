package com.flakyfilo.material.dto;

import com.flakyfilo.common.enums.MaterialUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MaterialCreateRequest(
        @NotNull Long storeId,
        @NotNull String name,
        @NotNull MaterialUnit unit,
        @NotNull @PositiveOrZero BigDecimal minStockThreshold,
        @Positive BigDecimal referenceQuantity,   // 예: 10000 (10kg를 g 단위로)
        @PositiveOrZero BigDecimal referencePrice, // 예: 30000 (그 10kg를 산 가격)
        Long supplierId, // 자유 텍스트가 아니라 Supplier의 id (필수 아님 - 아직 모를 수 있음)
        String note
) {
}