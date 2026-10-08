package com.flakyfilo.order.dto;

import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.order.Order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderChannel channel,
        LocalDate orderDate,
        int totalAmount,
        boolean cancelled,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getChannel(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.isCancelled(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
