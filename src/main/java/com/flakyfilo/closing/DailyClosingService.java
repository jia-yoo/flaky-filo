package com.flakyfilo.closing;

import com.flakyfilo.common.enums.ClosingActionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyClosingService {

    private final DailyClosingRepository closingRepository;
    private final ClosingActionLogRepository closingActionLogRepository;

    public boolean isClosed(Long storeId, LocalDate date) {
        return closingRepository.existsByStoreIdAndClosingDate(storeId, date);
    }

    /** 이 날짜가 이미 마감됐으면 예외 - 생산 등록/취소 전에 항상 이걸로 먼저 확인한다. */
    public void assertNotClosed(Long storeId, LocalDate date) {
        if (isClosed(storeId, date)) {
            throw new BusinessException(date + "는 이미 마감 처리된 날짜라 생산 등록/수정이 제한됩니다.");
        }
    }

    @Transactional
    public void closeDay(Long storeId, LocalDate date) {
        if (isClosed(storeId, date)) {
            throw new BusinessException("이미 마감 처리된 날짜입니다.");
        }
        closingRepository.save(DailyClosing.register(storeId, date));
    }

    /**
     * 마감 취소 - 마감됐다는 표시만 지우는 게 아니라, 그날 마감 때 했던 폐기/보류 처리를
     * ClosingActionLog 기록 기준으로 정확히 하나씩 되돌린다 (재고가 원래대로 복원됨).
     */
    @Transactional
    public void reopenDay(Long storeId, LocalDate date) {
        List<ClosingActionLog> actions = closingActionLogRepository.findByClosingDateAndNotReversed(date);

        for (ClosingActionLog action : actions) {
            Product product = action.getProduct();
            if (action.getActionType() == ClosingActionType.WASTE) {
                product.increaseStock(action.getQuantity()); // 폐기했던 걸 재고로 다시 되돌림
            } else { // RESERVE
                product.decreaseReservedStock(action.getQuantity()); // 보류에서 빼고
                product.increaseStock(action.getQuantity());          // 판매 재고로 되돌림
            }
            action.markReversed();
        }

        closingRepository.deleteByStoreIdAndClosingDate(storeId, date);
    }
}
