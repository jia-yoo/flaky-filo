package com.flakyfilo.common.enums;

/**
 * 생산 방식. NORMAL은 완제품 자체 레시피로 원재료부터 만드는 일반 생산,
 * CONVERSION은 다른 완제품의 보류 재고를 사용해 전환하는 생산.
 */
public enum ProductionType {
    NORMAL, CONVERSION
}