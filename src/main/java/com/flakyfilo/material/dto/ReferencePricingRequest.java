package com.flakyfilo.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** 기준 수량/가격("10kg에 3만원")을 갱신하는 전용 요청 - 구매 단가가 바뀌었을 때 사용 */
public record ReferencePricingRequest(
        @NotNull @Positive BigDecimal referenceQuantity,
        @NotNull @PositiveOrZero BigDecimal referencePrice
) {
}