package com.flakyfilo.closing.dto;

import java.time.LocalDate;

public record DailyClosingStatusResponse(
        LocalDate date,
        boolean closed
) {
}
