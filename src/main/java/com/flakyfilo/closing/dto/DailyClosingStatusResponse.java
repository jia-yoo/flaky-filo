package com.flakyfilo.closing.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * actions는 그날 마감 때 실제로 한 폐기/보류 처리 기록 (마감 취소로 되돌린 건 제외).
 * 이월은 재고를 그대로 두는 거라 기록이 없다 - 기록이 없는 완제품 = 이월로 보면 된다.
 */
public record DailyClosingStatusResponse(
        LocalDate date,
        boolean closed,
        List<ClosingActionResponse> actions
) {
}
