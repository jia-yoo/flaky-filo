package com.flakyfilo.closing;

import com.flakyfilo.common.enums.ClosingActionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import com.flakyfilo.product.ProductRepository;
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
    private final ProductRepository productRepository;

    public boolean isClosed(Long storeId, LocalDate date) {
        return closingRepository.existsByStoreIdAndClosingDate(storeId, date);
    }

    /** 이 날짜가 이미 마감됐으면 예외 - 생산 등록/취소 전에 항상 이걸로 먼저 확인한다. */
    public void assertNotClosed(Long storeId, LocalDate date) {
        if (isClosed(storeId, date)) {
            throw new BusinessException(date + "는 이미 마감 처리된 날짜라 생산 등록/수정이 제한됩니다.");
        }
    }

    /** 그날 마감 때 한 폐기/보류/이월 기록 (마감 취소로 되돌린 건 제외). 완제품 정보도 같이 fetch join으로 가져온다. */
    public List<ClosingActionLog> getActions(LocalDate date) {
        return closingActionLogRepository.findByClosingDateAndNotReversed(date);
    }

    /**
     * 날짜 마감. 폐기/보류 처리는 화면에서 이 호출 전에 이미 끝난 상태라,
     * 지금 판매 재고에 남아있는 수량 = 이월되는 수량이다 - 이걸 CARRY 기록으로 남겨서
     * 나중에 마감 기록을 볼 때 "보류 4개, 이월 1개"처럼 빠짐없이 보이게 한다.
     */
    @Transactional
    public void closeDay(Long storeId, LocalDate date) {
        if (isClosed(storeId, date)) {
            throw new BusinessException("이미 마감 처리된 날짜입니다.");
        }

        List<Product> scheduledProducts =
                productRepository.findByStoreIdAndActiveTrueAndInstantProductionFalse(storeId);
        for (Product product : scheduledProducts) {
            if (product.getCurrentStock() > 0) {
                closingActionLogRepository.save(ClosingActionLog.register(
                        product, date, ClosingActionType.CARRY, product.getCurrentStock()));
            }
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
            switch (action.getActionType()) {
                case WASTE -> product.increaseStock(action.getQuantity()); // 폐기했던 걸 재고로 다시 되돌림
                case RESERVE -> {
                    product.decreaseReservedStock(action.getQuantity()); // 보류에서 빼고
                    product.increaseStock(action.getQuantity());          // 판매 재고로 되돌림
                }
                case CARRY -> {
                    // 이월은 재고를 안 건드렸으니 되돌릴 것도 없음 - 기록만 reversed 처리
                }
            }
            action.markReversed();
        }

        closingRepository.deleteByStoreIdAndClosingDate(storeId, date);
    }
}
