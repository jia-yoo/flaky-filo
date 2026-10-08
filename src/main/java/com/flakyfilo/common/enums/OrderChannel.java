package com.flakyfilo.common.enums;

/**
 * 주문이 들어온 경로. NAVER/COUPANG/BAEMIN은 실제 API 연동 없이
 * "이 채널에서 들어온 주문"이라고 사람이 직접 등록하는 Mock 처리 - 도메인 로직은 STORE와 완전히 동일하게 취급.
 */
public enum OrderChannel {
    STORE, NAVER, COUPANG, BAEMIN
}
