package com.flakyfilo.material.dto;

import com.flakyfilo.common.enums.MaterialUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MaterialCreateRequest(
        @NotNull Long storeId,
        @NotNull String name,
        @NotNull MaterialUnit unit,
        @NotNull @PositiveOrZero BigDecimal minStockThreshold,
        @PositiveOrZero BigDecimal unitCost,
        Long supplierId, // 자유 텍스트가 아니라 Supplier의 id (필수 아님 - 아직 모를 수 있음)
        String note
) {
}