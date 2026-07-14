package com.flakyfilo.product.dto;

import jakarta.validation.Valid;

import java.util.List;

public record ConversionRecipeUpdateRequest(
        @Valid List<ConversionRecipeItemRequest> items
) {
}