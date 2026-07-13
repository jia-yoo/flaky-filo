package com.flakyfilo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재고 변동이 "왜" 일어났는지를 구조화한 코드.
 * reason(자유 텍스트)은 사람이 보는 부가 메모일 뿐이고,
 * 통계/집계(이번 달 생산 소진량, 폐기량 등)는 반드시 이 코드로만 걸러야 정확하다.
 * (자유 텍스트로 "생산"이 포함됐는지 문자열 검사하는 방식은 오타/문구 변경에 취약해서 지양)
 * <p>
 * 이 값은 사용자가 직접 고르는 게 아니라, 어떤 API/로직 경로로 들어왔는지에 따라
 * 서버가 자동으로 정한다.
 */
@Getter
@RequiredArgsConstructor
public enum StockReasonCode {
    PURCHASE("구매 입고"),                // IN  - 발주/구매로 인한 입고
    PRODUCTION_CONSUMPTION("생산 소진"), // OUT - 생산 시 BOM 기준 자동 소진
    DISPOSAL("폐기·손실"),                // OUT - 유통기한 만료, 파손 등 사용자가 직접 등록한 폐기·손실
    PERIODIC_STOCKTAKE("정기 실사"),      // ADJUST_UP/DOWN - 정기 실사(월말 재고조사 등)로 인한 보정
    AD_HOC_CORRECTION("수시 보정");       // ADJUST_UP/DOWN - 오입력 정정 등 수시로 바로잡은 보정

    private final String label;
}