package com.flakyfilo.closing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DailyClosingRequest(
        @NotNull LocalDate date
) {
}
