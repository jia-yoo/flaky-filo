package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecipeItemRequest(
        @NotNull Long materialId,
        @NotNull @Positive BigDecimal batchQuantity // 배치 전체 기준 필요량 (개당 아님)
) {
}
