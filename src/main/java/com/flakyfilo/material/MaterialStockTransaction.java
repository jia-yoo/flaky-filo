package com.flakyfilo.material;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.enums.StockReasonCode;
import com.flakyfilo.common.enums.StockTransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 원재료 재고는 이 테이블을 통해서만 변경된다 (Material.currentStock 직접 수정 금지).
 * 나중에 "왜 재고가 안 맞는지" 추적할 때 이 이력이 근거가 된다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "material_stock_transaction")
public class MaterialStockTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material; // 어떤 원재료의 변동인지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransactionType type; // 방향(IN/OUT/ADJUST_UP/ADJUST_DOWN)

    // 구조화된 사유. 사용자가 고르는 값이 아니라, 어떤 로직 경로로 들어왔는지에 따라 서버가 결정.
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private StockReasonCode reasonCode;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity; // 얼마나 변동됐는지 (항상 양수, 방향은 type이 결정)

    @Column(length = 200)
    private String reason; // 왜 변동됐는지 (예: "정기 발주 입고", "생산 자동 차감")

    @Column(nullable = false)
    private LocalDate transactionDate; // 변동이 일어난 날짜

    @Builder(access = AccessLevel.PRIVATE)
    private MaterialStockTransaction(Material material, StockTransactionType type, StockReasonCode reasonCode,
                                     BigDecimal quantity, String reason, LocalDate transactionDate) {
        this.material = material;
        this.type = type;
        this.reasonCode = reasonCode;
        this.quantity = quantity;
        this.reason = reason;
        this.transactionDate = transactionDate != null ? transactionDate : LocalDate.now();
    }

    public static MaterialStockTransaction register(Material material, StockTransactionType type,
                                                    StockReasonCode reasonCode, BigDecimal quantity,
                                                    String reason, LocalDate transactionDate) {
        return MaterialStockTransaction.builder()
                .material(material)
                .type(type)
                .reasonCode(reasonCode)
                .quantity(quantity)
                .reason(reason)
                .transactionDate(transactionDate)
                .build();
    }
}
