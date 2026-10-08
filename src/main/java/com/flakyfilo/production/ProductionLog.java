package com.flakyfilo.production;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.enums.SourceStockType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 생산 이벤트. productionType이 CONVERSION이면 sourceProduct(원본 완제품)가 채워지고,
 * sourceStockType으로 그 원본의 "보류재고"에서 가져온 건지 "당일 생산분(현재고)"에서 가져온 건지 구분한다.
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

    // CONVERSION일 때만 값이 있음 - 원본 완제품 (예: 크로와상)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_product_id")
    private Product sourceProduct;

    // CONVERSION일 때만 값이 있음 - 원본의 보류재고를 썼는지, 당일 생산분(현재고)을 썼는지
    @Enumerated(EnumType.STRING)
    @Column(name = "source_stock_type", length = 20)
    private SourceStockType sourceStockType;

    @Column(length = 200)
    private String note;

    /**
     * 즉석메뉴 "빠른 기록"으로 만들어진 건인지. true면 애초에 Product.currentStock을
     * 늘리지 않았으므로(즉석메뉴는 재고를 안 쌓아둠), cancel()에서 재고를 되돌릴 때
     * currentStock 복원 단계는 건너뛰어야 한다 - 안 그러면 늘린 적 없는 재고를 깎아버리게 됨.
     */
    @Column(name = "instant_record", nullable = false)
    private boolean instantRecord = false;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Builder(access = AccessLevel.PRIVATE)
    private ProductionLog(Product product, int producedQuantity, LocalDate productionDate,
                          ProductionType productionType, Product sourceProduct, SourceStockType sourceStockType,
                          String note, boolean instantRecord) {
        this.product = product;
        this.producedQuantity = producedQuantity;
        this.productionDate = productionDate;
        this.productionType = productionType;
        this.sourceProduct = sourceProduct;
        this.sourceStockType = sourceStockType;
        this.note = note;
        this.instantRecord = instantRecord;
    }

    public static ProductionLog register(Product product, int producedQuantity, LocalDate productionDate,
                                         ProductionType productionType, Product sourceProduct,
                                         SourceStockType sourceStockType, String note) {
        return registerInternal(product, producedQuantity, productionDate, productionType,
                sourceProduct, sourceStockType, note, false);
    }

    /** 즉석메뉴 빠른 기록 전용 - Product.currentStock을 안 늘리는 생산 이벤트. */
    public static ProductionLog registerInstant(Product product, int producedQuantity, LocalDate productionDate,
                                                ProductionType productionType, Product sourceProduct,
                                                SourceStockType sourceStockType, String note) {
        return registerInternal(product, producedQuantity, productionDate, productionType,
                sourceProduct, sourceStockType, note, true);
    }

    private static ProductionLog registerInternal(Product product, int producedQuantity, LocalDate productionDate,
                                                  ProductionType productionType, Product sourceProduct,
                                                  SourceStockType sourceStockType, String note,
                                                  boolean instantRecord) {
        Validate.strictlyPositive(producedQuantity, "생산 수량");
        if (productionType == ProductionType.CONVERSION) {
            if (sourceProduct == null) {
                throw new BusinessException("전환 생산은 원본 완제품을 선택해야 합니다.");
            }
            if (sourceStockType == null) {
                throw new BusinessException("전환 생산은 보류재고/당일생산분 중 어디서 가져올지 선택해야 합니다.");
            }
        }
        return ProductionLog.builder()
                .product(product)
                .producedQuantity(producedQuantity)
                .productionDate(productionDate != null ? productionDate : LocalDate.now())
                .productionType(productionType)
                .sourceProduct(sourceProduct)
                .sourceStockType(sourceStockType)
                .note(note)
                .instantRecord(instantRecord)
                .build();
    }

    public void cancel() {
        if (this.cancelled) {
            throw new BusinessException("이미 취소된 생산 기록입니다.");
        }
        this.cancelled = true;
    }
}