package com.flakyfilo.material.dto;

import com.flakyfilo.common.enums.StockReasonCode;
import com.flakyfilo.common.enums.StockTransactionType;
import com.flakyfilo.material.MaterialStockTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MaterialStockTransactionResponse(
        Long id,
        Long materialId,
        StockTransactionType type,
        StockReasonCode reasonCode,
        String reasonLabel, // reasonCode.getLabel()
        BigDecimal quantity,
        String reason,
        LocalDate transactionDate,
        LocalDateTime createdAt
) {
    public static MaterialStockTransactionResponse from(MaterialStockTransaction transaction) {
        return new MaterialStockTransactionResponse(
                transaction.getId(),
                transaction.getMaterial().getId(),
                transaction.getType(),
                transaction.getReasonCode(),
                transaction.getReasonCode().getLabel(),
                transaction.getQuantity(),
                transaction.getReason(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }
}