package com.flakyfilo.common.enums;

/**
 * 원재료/완제품 공통 재고 변동 타입.
 * IN: 입고(원재료 발주 입고, 완제품 생산)
 * OUT: 출고(원재료 소진, 완제품 판매)
 * ADJUST: 실사 등에 의한 수동 보정
 */
public enum StockTransactionType {
    IN, OUT, ADJUST_UP, ADJUST_DOWN
}
