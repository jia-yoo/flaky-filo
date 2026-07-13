package com.flakyfilo.production;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * "아침에 빵 30개 구웠다"는 생산 이벤트 자체.
 * 오입력했을 때는 이 기록을 직접 고치지 않고 cancel()로 취소한 뒤 다시 등록한다
 * (재고 이력은 append-only로 유지 - Material 쪽과 같은 원칙).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_log")
public class ProductionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "produced_quantity", nullable = false)
    private int producedQuantity;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Builder(access = AccessLevel.PRIVATE)
    private ProductionLog(Product product, int producedQuantity, LocalDate productionDate, String note) {
        this.product = product;
        this.producedQuantity = producedQuantity;
        this.productionDate = productionDate;
        this.note = note;
    }

    public static ProductionLog register(Product product, int producedQuantity, LocalDate productionDate, String note) {
        if (producedQuantity <= 0) {
            throw new BusinessException("생산 수량은 0보다 커야 합니다.");
        }
        return ProductionLog.builder()
                .product(product)
                .producedQuantity(producedQuantity)
                .productionDate(productionDate != null ? productionDate : LocalDate.now())
                .note(note)
                .build();
    }

    public void cancel() {
        if (this.cancelled) {
            throw new BusinessException("이미 취소된 생산 기록입니다.");
        }
        this.cancelled = true;
    }
}
