package com.flakyfilo.product.dto;

import com.flakyfilo.product.ProductRecipe;

import java.math.BigDecimal;

public record RecipeItemResponse(
        Long materialId,
        String materialName,
        String unit,
        BigDecimal batchQuantity,     // 배치 전체 기준 필요량 (입력값 그대로)
        BigDecimal perUnitQuantity,   // 완제품 1개당 필요량 (배치량 ÷ 나오는 개수)
        BigDecimal costContribution   // 완제품 1개 기준 이 재료의 원가 기여분
) {
    public static RecipeItemResponse from(ProductRecipe recipe) {
        return new RecipeItemResponse(
                recipe.getMaterial().getId(),
                recipe.getMaterial().getName(),
                recipe.getMaterial().getUnit().name(),
                recipe.getBatchQuantity(),
                recipe.calculatePerUnitQuantity(),
                recipe.calculateCostContribution()
        );
    }
}
