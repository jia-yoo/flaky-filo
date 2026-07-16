package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotNull Long storeId,
        @NotBlank String name,
        String category,
        @PositiveOrZero int price,
        Integer yieldCount,          // null이면 서버가 기본값 1로 처리
        BigDecimal overheadRate,     // null이면 서버가 기본값 0.10으로 처리
        BigDecimal targetCostRatio,  // null이면 서버가 기본값 0.40으로 처리
        Boolean active,              // null이면 기본값 true (판매중)
        Boolean autoDisposeIfUnsold,  // null이면 기본값 true (이월 메뉴)
        Boolean instantProduction // null이면 기본값 false (일반 완제품)
) {
}