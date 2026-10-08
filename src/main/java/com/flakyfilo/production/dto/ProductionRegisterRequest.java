package com.flakyfilo.production.dto;

import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.enums.SourceStockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ProductionRegisterRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer producedQuantity,
        LocalDate productionDate,
        @NotNull ProductionType productionType, // NORMAL 또는 CONVERSION
        Long sourceProductId,          // CONVERSION일 때만 필수
        SourceStockType sourceStockType, // CONVERSION일 때만 필수 - RESERVED(보류재고) 또는 CURRENT(당일생산분)
        String note
) {
}
