package com.flakyfilo.closing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** 마감 처리(폐기/보류) 전용 요청 - closingDate가 있어야 나중에 "마감 취소"로 정확히 되돌릴 수 있다. */
public record ClosingStockActionRequest(
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate closingDate
) {
}
