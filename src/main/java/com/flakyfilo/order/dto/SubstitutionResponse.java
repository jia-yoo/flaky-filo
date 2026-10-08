package com.flakyfilo.order.dto;

import com.flakyfilo.order.OrderItemSubstitution;

public record SubstitutionResponse(
        Long substituteProductId,
        String substituteProductName,
        int quantity
) {
    public static SubstitutionResponse from(OrderItemSubstitution sub) {
        return new SubstitutionResponse(
                sub.getSubstituteProduct().getId(),
                sub.getSubstituteProduct().getName(),
                sub.getQuantity()
        );
    }
}
