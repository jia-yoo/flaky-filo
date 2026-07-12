package com.flakyfilo.material.dto;

import com.flakyfilo.common.StockTransactionType;
import com.flakyfilo.material.MaterialStockTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialStockTransactionResponse(
        Long id,
        Long materialId,
        StockTransactionType type,
        BigDecimal quantity,
        String reason,
        LocalDate transactionDate
) {
    public static MaterialStockTransactionResponse from(MaterialStockTransaction transaction) {
        return new MaterialStockTransactionResponse(
                transaction.getId(),
                transaction.getMaterial().getId(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getReason(),
                transaction.getTransactionDate()
        );
    }
}