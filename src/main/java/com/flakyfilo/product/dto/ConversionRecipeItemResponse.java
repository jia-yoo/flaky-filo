package com.flakyfilo.product.dto;

import com.flakyfilo.product.ProductConversionRecipe;

import java.math.BigDecimal;

public record ConversionRecipeItemResponse(
        Long sourceProductId,
        String sourceProductName,
        Long materialId,
        String materialName,
        String unit,
        BigDecimal perUnitQuantity
) {
    public static ConversionRecipeItemResponse from(ProductConversionRecipe recipe) {
        return new ConversionRecipeItemResponse(
                recipe.getSourceProduct().getId(),
                recipe.getSourceProduct().getName(),
                recipe.getMaterial().getId(),
                recipe.getMaterial().getName(),
                recipe.getMaterial().getUnit().name(),
                recipe.getPerUnitQuantity()
        );
    }
}