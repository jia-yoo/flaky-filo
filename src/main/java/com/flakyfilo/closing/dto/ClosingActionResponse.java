package com.flakyfilo.closing.dto;

import com.flakyfilo.closing.ClosingActionLog;
import com.flakyfilo.common.enums.ClosingActionType;

public record ClosingActionResponse(
        Long productId,
        String productName,
        ClosingActionType actionType,
        int quantity
) {
    public static ClosingActionResponse from(ClosingActionLog log) {
        return new ClosingActionResponse(
                log.getProduct().getId(),
                log.getProduct().getName(),
                log.getActionType(),
                log.getQuantity()
        );
    }
}
