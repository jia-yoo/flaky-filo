package com.flakyfilo.production.dto;

import com.flakyfilo.common.enums.ProductionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ProductionRegisterRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer producedQuantity,
        LocalDate productionDate,
        @NotNull ProductionType productionType, // NORMAL 또는 CONVERSION
        Long sourceProductId, // CONVERSION일 때만 필수 (보류 재고를 제공하는 원본 완제품)
        String note
) {
}
