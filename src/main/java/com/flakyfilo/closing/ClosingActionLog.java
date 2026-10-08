package com.flakyfilo.closing;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.enums.ClosingActionType;
import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 마감 처리(폐기/보류) 한 건을 정확히 기록해둔 것. "마감 취소"를 누르면
 * 이 기록 기준으로 정확히 반대 방향으로 되돌린다 (ProductionMaterialUsage와 같은 원리).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "closing_action_log")
public class ClosingActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "closing_date", nullable = false)
    private LocalDate closingDate;

    // columnDefinition 없이 두면 Hibernate가 MySQL에 enum('RESERVE','WASTE') 타입으로 만들어서,
    // 나중에 enum 값을 추가하면(CARRY 등) ddl-auto: update로는 컬럼이 안 바뀌어 저장이 실패한다 - 그래서 varchar로 고정
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private ClosingActionType actionType;

    @Column(nullable = false)
    private int quantity;

    // 마감 취소로 이미 되돌려졌으면 true - 중복으로 두 번 되돌리는 것 방지
    @Column(nullable = false)
    private boolean reversed = false;

    @Builder(access = AccessLevel.PRIVATE)
    private ClosingActionLog(Product product, LocalDate closingDate, ClosingActionType actionType, int quantity) {
        this.product = product;
        this.closingDate = closingDate;
        this.actionType = actionType;
        this.quantity = quantity;
    }

    public static ClosingActionLog register(Product product, LocalDate closingDate, ClosingActionType actionType, int quantity) {
        return ClosingActionLog.builder()
                .product(product).closingDate(closingDate).actionType(actionType).quantity(quantity).build();
    }

    public void markReversed() {
        this.reversed = true;
    }
}
