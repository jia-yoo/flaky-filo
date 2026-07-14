package com.flakyfilo.product.dto;

import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.product.ProductChannelPrice;

public record ChannelPriceResponse(
        OrderChannel channel,
        int price
) {
    public static ChannelPriceResponse from(ProductChannelPrice entity) {
        return new ChannelPriceResponse(entity.getChannel(), entity.getPrice());
    }
}