package com.flakyfilo.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderLineRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity,
        @Valid List<SubstitutionLineRequest> substitutions // 없으면 빈 리스트로 취급
) {
}
