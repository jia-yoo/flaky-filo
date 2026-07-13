package com.flakyfilo.production.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ProductionRegisterRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer producedQuantity,
        LocalDate productionDate, // null이면 서비스가 오늘 날짜로 처리
        String note
) {
}
