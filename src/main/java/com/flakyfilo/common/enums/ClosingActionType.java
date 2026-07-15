package com.flakyfilo.common.enums;

/** 마감 처리 때 한 행위 종류. 마감 취소 시 이 타입에 따라 반대로 되돌리는 방법이 달라진다. */
public enum ClosingActionType {
    WASTE,   // 즉시 폐기 - 되돌릴 땐 재고를 다시 늘려주면 됨
    RESERVE  // 보류로 전환 - 되돌릴 땐 보류재고를 줄이고 판매재고를 다시 늘려주면 됨
}