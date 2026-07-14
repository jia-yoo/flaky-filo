package com.flakyfilo.production;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 생산 이벤트. productionType이 CONVERSION이면 sourceProduct(보류 재고를 제공한 완제품)가 채워진다.
 * 오입력했을 때는 이 기록을 직접 고치지 않고 cancel()로 취소한 뒤 다시 등록한다.
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
    private Product product; // 생산 결과물 (완성된 완제품)

    @Column(name = "produced_quantity", nullable = false)
    private int producedQuantity;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_type", nullable = false, length = 20)
    private ProductionType productionType;

    // CONVERSION일 때만 값이 있음 - 보류 재고를 제공한 원본 완제품 (예: 크로와상)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_product_id")
    private Product sourceProduct;

    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Builder(access = AccessLevel.PRIVATE)
    private ProductionLog(Product product, int producedQuantity, LocalDate productionDate,
                          ProductionType productionType, Product sourceProduct, String note) {
        this.product = product;
        this.producedQuantity = producedQuantity;
        this.productionDate = productionDate;
        this.productionType = productionType;
        this.sourceProduct = sourceProduct;
        this.note = note;
    }

    public static ProductionLog register(Product product, int producedQuantity, LocalDate productionDate,
                                         ProductionType productionType, Product sourceProduct, String note) {
        if (producedQuantity <= 0) {
            throw new BusinessException("생산 수량은 0보다 커야 합니다.");
        }
        if (productionType == ProductionType.CONVERSION && sourceProduct == null) {
            throw new BusinessException("전환 생산은 원본 완제품을 선택해야 합니다.");
        }
        return ProductionLog.builder()
                .product(product)
                .producedQuantity(producedQuantity)
                .productionDate(productionDate != null ? productionDate : LocalDate.now())
                .productionType(productionType)
                .sourceProduct(sourceProduct)
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