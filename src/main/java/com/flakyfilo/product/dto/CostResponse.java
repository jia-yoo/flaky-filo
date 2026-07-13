package com.flakyfilo.product.dto;

import com.flakyfilo.product.ProductService;

import java.math.BigDecimal;

public record CostResponse(
        BigDecimal rawCost,      // 원가 (기타경비 포함 전)
        BigDecimal overhead,     // 기타경비
        BigDecimal totalCost,    // 총원가 = 원가 + 기타경비
        BigDecimal costRatio,    // 원가율 = 원가 ÷ 판매가 (판매가 0이면 null)
        BigDecimal minPrice,     // 최소판매가 = 총원가 ÷ 목표원가율
        BigDecimal margin,       // 판매가 - 총원가
        int price
) {
    public static CostResponse from(ProductService.CostResult result) {
        return new CostResponse(
                result.rawCost(), result.overhead(), result.totalCost(),
                result.costRatio(), result.minPrice(), result.margin(), result.price());
    }
}
