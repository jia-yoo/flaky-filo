package com.flakyfilo.common;

import com.flakyfilo.common.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 여러 Entity에서 반복되는 기본 검증 로직을 모아둔 공통 유틸리티.
 * 특정 Entity에 종속된 로직이 아니라 "값 자체의 규칙"만 검사하므로
 * static 메서드로 어디서든 재사용 가능하게 뺐다.
 */
public class Validate {

    private Validate() {
        // 유틸리티 클래스라 인스턴스를 만들 필요가 없음 - 생성자를 막아서 실수로 new Validate() 하는 걸 방지
    }

    /** 문자열이 null이거나 빈 값이면 예외 */
    public static void notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + "은(는) 비어있을 수 없습니다.");
        }
    }

    /** 진짜 양수만 허용 (0 제외) - 수량처럼 "0은 의미 없는" 값에 사용 */
    public static void strictlyPositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(fieldName + "은(는) 0보다 커야 합니다.");
        }
    }

    /** 진짜 양수만 허용 (0 제외) - int 버전. Product의 개수(quantity)처럼 BigDecimal이 아닌 값에 사용 */
    public static void strictlyPositive(int value, String fieldName) {
        if (value <= 0) {
            throw new BusinessException(fieldName + "은(는) 0보다 커야 합니다.");
        }
    }

    /** 0 이상이면 허용 - 실사값, 임계치처럼 "0도 정상적인" 값에 사용 */
    public static void notNegative(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(fieldName + "은(는) 0 이상이어야 합니다.");
        }
    }

    /** 0 이상이면 허용 - int 버전 */
    public static void notNegative(int value, String fieldName) {
        if (value < 0) {
            throw new BusinessException(fieldName + "은(는) 0 이상이어야 합니다.");
        }
    }
}