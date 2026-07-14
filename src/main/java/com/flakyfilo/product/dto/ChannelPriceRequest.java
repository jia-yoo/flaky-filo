package com.flakyfilo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChannelPriceRequest(
        @NotNull @PositiveOrZero Integer price
) {
}