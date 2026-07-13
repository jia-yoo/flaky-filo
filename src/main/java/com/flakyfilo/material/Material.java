package com.flakyfilo.material;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.MaterialUnit;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.supplier.Supplier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "material")
public class Material extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialUnit unit;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "min_stock_threshold", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStockThreshold = BigDecimal.ZERO;

    /**
     * 단가를 사람이 직접 계산해서 넣는 대신("3만원÷10kg=3000원짜리 미리 계산해서 입력"),
     * "10kg에 3만원"이라는 실제 구매 정보 그대로("기준 수량" + "기준 가격")를 저장한다.
     * 단가(1단위당 가격)는 저장하지 않고, 필요할 때마다 이 두 값으로 계산한다 (getUnitCost() 참고).
     */
    @Column(name = "reference_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal referenceQuantity = BigDecimal.ONE;

    @Column(name = "reference_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal referencePrice = BigDecimal.ZERO;

    // 자유 텍스트(String)가 아니라 Supplier 기준정보를 참조 - null 허용(구매처 아직 모를 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(length = 500)
    private String note;

    @Builder(access = AccessLevel.PRIVATE)
    private Material(Long storeId, String name, MaterialUnit unit,
                     BigDecimal minStockThreshold, BigDecimal referenceQuantity, BigDecimal referencePrice,
                     Supplier supplier, String note) {
        this.storeId = storeId;
        this.name = name;
        this.unit = unit;
        this.minStockThreshold = minStockThreshold;
        this.referenceQuantity = referenceQuantity != null ? referenceQuantity : BigDecimal.ONE;
        this.referencePrice = referencePrice != null ? referencePrice : BigDecimal.ZERO;
        this.supplier = supplier;
        this.note = note;
    }

    public static Material register(Long storeId, String name, MaterialUnit unit, BigDecimal minStockThreshold,
                                    BigDecimal referenceQuantity, BigDecimal referencePrice,
                                    Supplier supplier, String note) {
        if (referenceQuantity != null && referenceQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("기준 수량은 0보다 커야 합니다.");
        }
        return Material.builder()
                .storeId(storeId)
                .name(name)
                .unit(unit)
                .minStockThreshold(minStockThreshold)
                .referenceQuantity(referenceQuantity)
                .referencePrice(referencePrice)
                .supplier(supplier)
                .note(note)
                .build();
    }

    /** 1단위당 가격. 저장된 값이 아니라 "기준 가격 ÷ 기준 수량"으로 그때그때 계산한다. */
    public BigDecimal getUnitCost() {
        return referencePrice.divide(referenceQuantity, 4, RoundingMode.HALF_UP);
    }

    public void increaseStock(BigDecimal quantity) {
        Validate.strictlyPositive(quantity, "수량");
        this.currentStock = this.currentStock.add(quantity);
    }

    public void decreaseStock(BigDecimal quantity) {
        Validate.strictlyPositive(quantity, "수량");
        if (this.currentStock.compareTo(quantity) < 0) {
            throw new BusinessException(
                    "원재료 재고 부족: " + this.name + " (현재 " + this.currentStock + this.unit
                            + ", 요청 " + quantity + this.unit + ")");
        }
        this.currentStock = this.currentStock.subtract(quantity);
    }

    public boolean isBelowThreshold() {
        return this.currentStock.compareTo(this.minStockThreshold) <= 0;
    }

    /** 기준 수량/가격 갱신. 예: "이번엔 10kg에 3만2천원으로 올랐다"처럼 구매 단가가 바뀌었을 때 사용. */
    public void updateReferencePricing(BigDecimal referenceQuantity, BigDecimal referencePrice) {
        if (referenceQuantity == null || referenceQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("기준 수량은 0보다 커야 합니다.");
        }
        Validate.notNegative(referencePrice, "기준 가격");
        this.referenceQuantity = referenceQuantity;
        this.referencePrice = referencePrice;
    }

    public void updateInfo(String name, MaterialUnit unit, BigDecimal minStockThreshold,
                           Supplier supplier, String note) {
        Validate.notBlank(name, "이름");
        Validate.notNegative(minStockThreshold, "재고 부족 기준"); // 기준을 0으로 두는 것도 정상적인 설정
        this.name = name;
        this.unit = unit;
        this.minStockThreshold = minStockThreshold;
        this.supplier = supplier;
        this.note = note;
    }

    public BigDecimal applyStocktake(BigDecimal actualStock) {
        Validate.notNegative(actualStock, "실사 재고량"); // 실사값 0은 "재고가 실제로 없더라"는 정상 상황
        BigDecimal delta = actualStock.subtract(this.currentStock);
        this.currentStock = actualStock;
        return delta;
    }
}