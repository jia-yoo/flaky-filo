package com.flakyfilo.order.dto;

import com.flakyfilo.common.enums.OrderChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record OrderCreateRequest(
        @NotNull OrderChannel channel,
        LocalDate orderDate, // null이면 오늘로 처리
        @NotEmpty @Valid List<OrderLineRequest> items
) {
}
