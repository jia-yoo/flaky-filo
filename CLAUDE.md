# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

플레키필로(Flaky Filo)는 실제 빵집 한 곳에서 직접 쓰는 재고·생산·주문 관리 앱이다. 원재료 → 레시피(BOM) → 완제품 생산 → 판매/마감까지의 재고 흐름을 추적한다. 지금은 매장이 하나뿐이라 서비스에서 `DEFAULT_STORE_ID = 1L`로 고정해 쓴다.

## 명령어

```bash
docker compose up -d          # 로컬 MySQL 8.0 실행 (localhost:3306, DB: flaky-filo, root / flaky-filo1234)
./gradlew bootRun             # 앱 실행 → http://localhost:8080
./gradlew build               # 빌드 + 테스트
./gradlew test --tests "com.flakyfilo.SomeTest"   # 테스트 하나만 실행
```

- 앱을 띄우기 전에 반드시 Docker로 MySQL부터 띄운다. 스키마는 `ddl-auto: update`로 엔티티에서 자동 생성된다. 마이그레이션 도구는 없다.
- 테스트는 H2 인메모리 DB를 쓰도록 의존성만 잡혀 있고, `src/test`는 아직 없다.
- Java 17, Spring Boot 3.3, JPA, Lombok 사용.

## 커밋 컨벤션

커밋 메시지는 **한국어**로, 이모지 + 타입 접두어를 붙여 쓴다.
- `✨feat: ...` 기능 추가 / `🐛fix: ...` 버그 수정 / `🔧refactor: ...` 리팩터링 / `chore: ...` 기타

## 아키텍처

### 백엔드 구조 (`src/main/java/com/flakyfilo`)
도메인별 패키지(`material`, `product`, `production`, `closing`, `order`, `supplier`) 안에 Entity / Repository / Service / Controller / `dto`가 함께 있다.

- **엔티티가 스스로 규칙을 지킨다**: 엔티티는 `protected` 기본 생성자 + `private @Builder` + 정적 팩토리 `register(...)`로 생성하고, 재고 증감(`increaseStock`, `decreaseStock`, `decreaseReservedStock` 등)과 검증은 엔티티 메서드 안에서 처리한다. 값 검증은 `common.Validate`, id 조회 후 없으면 예외는 `common.EntityFinder.findOrThrow`를 쓴다.
- **예외 처리**: 예상 가능한 업무 오류는 `BusinessException`(한국어 메시지)을 던지면 `GlobalExceptionHandler`가 400 + `{message}`로 응답한다.
- 서비스는 클래스에 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`을 붙인다.
- 고정 코드값(단위, 채널, 재고 사유 등)은 테이블 대신 `common/enums`의 enum으로 관리한다.
- **엔티티의 enum 필드에는 반드시 `@Enumerated(EnumType.STRING)` + `@Column(..., columnDefinition = "varchar(N)")`을 붙인다.** 빠뜨리면 Hibernate가 MySQL `enum(...)` 타입으로 컬럼을 만든다. 그러면 나중에 enum 값을 추가해도 `ddl-auto: update`가 컬럼을 바꾸지 않아서 저장이 실패한다.

### 재고 흐름 (여러 서비스에 걸쳐 있음)
- **원재료(Material)**: 모든 재고 변동은 `MaterialStockTransaction`에 `StockTransactionType`(IN/OUT/ADJUST_UP/DOWN) + `StockReasonCode`로 남긴다. 사유 코드는 사용자가 고르지 않고, 어떤 API 경로로 들어왔는지에 따라 서버가 정한다. 통계는 자유 텍스트 `reason`이 아니라 이 코드로만 집계한다.
- **완제품(Product)**: `currentStock`(판매 재고)과 `reservedStock`(마감 때 안 팔려서 보류해 둔 재고)이 따로 있다.
- **생산(ProductionService)**:
  - `NORMAL` 생산은 `ProductRecipe`(배치 기준 재료량 ÷ `yieldCount`)로 원재료를 차감한다.
  - `CONVERSION` 생산은 원본 완제품 재고(`SourceStockType` RESERVED/CURRENT)를 차감하고 `ProductConversionRecipe`로 추가 재료를 차감한다.
  - 차감한 재료량은 `ProductionMaterialUsage`에 스냅샷으로 남겨두고, 생산 취소 시 레시피가 아니라 이 스냅샷 기준으로 복원한다.
  - `instantProduction` 메뉴(주문 받을 때 바로 만드는 메뉴)는 `registerInstant`로 기록한다. 원재료만 차감하고 `currentStock`은 늘리지 않으며, 마감 체크도 하지 않는다. 프론트에서는 모든 화면에 떠 있는 플로팅 위젯(`common.js`)으로 기록한다.
- **마감(DailyClosingService)**: 마감된 날짜에는 일반 생산 등록/취소가 막힌다(`assertNotClosed`). 마감 때 한 폐기(WASTE)·보류(RESERVE) 처리는 `ClosingActionLog`에 남기고, 마감 취소(`reopenDay`) 시 이 로그를 보고 재고를 정확히 되돌린다.
- **주문(OrderService)**: 상태값 없이 등록하는 즉시 판매 확정이다. 즉석 메뉴이거나 주문 날짜가 이미 마감됐으면 재고를 건드리지 않는다(매출 기록만). 그 외에는 바로 재고를 차감하고, 대체발송이 있으면 대체 완제품 쪽에서 차감한다. 가격은 `ProductChannelPrice`(채널별 가격)를 먼저 보고, 없으면 기본 `price`를 쓴다. NAVER/COUPANG/BAEMIN 채널은 실제 API 연동 없이 사람이 직접 등록하는 방식이다.

### 프론트엔드 (`src/main/resources/static`)
빌드 도구 없이 순수 정적 HTML + Vanilla JS로 만들었고, Spring Boot가 그대로 서빙한다. 화면마다 `xxx.html` + `js/xxx.js`가 짝을 이룬다. 공통 상단 네비게이션, 토스트, dialog 바깥 클릭 시 닫기, 즉석메뉴 위젯은 `js/common.js`에 있다. 새 화면을 추가하면 `common.js`의 `TOPBAR_HTML`에 링크를 넣고, HTML에는 `<div id="topbar-placeholder">`와 `common.js`를 포함한다.

## 작성 스타일

코드 주석과 사용자에게 보이는 메시지는 한국어로 쓰고, 기존 코드처럼 "왜 이렇게 했는지"를 주석으로 충분히 남긴다.
