package com.flakyfilo.common.enums;

/**
 * 전환 생산(CONVERSION)에서 원본 완제품의 재고를 어디서 가져오는지.
 * 보류재고(어제 남겨둔 것)일 수도 있고, 당일 생산분(오늘 막 만든 것, currentStock)일 수도 있다 -
 * 둘 중 뭘 쓰는지는 그때그때 다르므로, 매번 사용자가 선택해서 넘겨준다.
 */
public enum SourceStockType {
    RESERVED, // 보류재고에서 차감
    CURRENT   // 당일 생산분(currentStock)에서 차감
}
