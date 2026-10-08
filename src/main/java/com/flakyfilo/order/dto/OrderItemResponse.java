package com.flakyfilo.order.dto;

import com.flakyfilo.order.OrderItem;

import java.util.List;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        int unitPrice,
        int subtotal,
        boolean stockAffected,
        List<SubstitutionResponse> substitutions
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.isStockAffected(),
                item.getSubstitutions().stream().map(SubstitutionResponse::from).toList()
        );
    }
}
