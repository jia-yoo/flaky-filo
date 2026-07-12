package com.flakyfilo.common;

/**
 * 원재료 계량 단위. 종류가 거의 안 바뀌는 고정값이라 테이블(공통코드) 대신 enum으로 관리.
 * DB에는 이름 그대로("KG", "G" 등) 저장됨 (@Enumerated(EnumType.STRING) 적용 전제).
 */
public enum MaterialUnit {
    G, KG, L, ML, EA
}
