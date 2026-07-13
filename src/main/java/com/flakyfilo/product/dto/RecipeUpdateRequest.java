package com.flakyfilo.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecipeUpdateRequest(
        @NotEmpty @Valid List<RecipeItemRequest> items
) {
}