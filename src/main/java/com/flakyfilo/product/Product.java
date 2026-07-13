package com.flakyfilo.product;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private int price;

    @Column(name = "current_stock", nullable = false)
    private int currentStock = 0;

    // 레시피 한 번 만들 때 나오는 완제품 개수. 재료는 "전체 배치 양"으로 입력하고,
    // 이 값으로 나눠서 "완제품 1개당 원가"를 계산한다.
    @Column(name = "yield_count", nullable = false)
    private int yieldCount = 1;

    // 기타경비율 (전기세/월세 등 레시피에 안 잡히는 비용을 원가의 몇 %로 반영할지). 기본 10%.
    @Column(name = "overhead_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal overheadRate = new BigDecimal("0.10");

    // 목표 원가율 (이 비율을 넘지 않으려면 최소 얼마를 받아야 하는지 계산할 때 사용). 기본 40%.
    @Column(name = "target_cost_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal targetCostRatio = new BigDecimal("0.40");

    @Builder(access = AccessLevel.PRIVATE)
    private Product(Long storeId, String name, String category, int price,
                    int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio) {
        this.storeId = storeId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.yieldCount = yieldCount;
        this.overheadRate = overheadRate;
        this.targetCostRatio = targetCostRatio;
    }

    public static Product register(Long storeId, String name, String category, int price,
                                    Integer yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio) {
        Validate.notBlank(name, "완제품명");
        return Product.builder()
                .storeId(storeId)
                .name(name)
                .category(category)
                .price(price)
                .yieldCount(yieldCount != null ? yieldCount : 1)
                .overheadRate(overheadRate != null ? overheadRate : new BigDecimal("0.10"))
                .targetCostRatio(targetCostRatio != null ? targetCostRatio : new BigDecimal("0.40"))
                .build();
    }

    public void updateInfo(String name, String category, int price,
                           int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio) {
        Validate.notBlank(name, "완제품명");
        if (yieldCount < 1) {
            throw new BusinessException("나오는 개수는 1개 이상이어야 합니다.");
        }
        this.name = name;
        this.category = category;
        this.price = price;
        this.yieldCount = yieldCount;
        this.overheadRate = overheadRate;
        this.targetCostRatio = targetCostRatio;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("수량은 0보다 커야 합니다.");
        }
        this.currentStock += quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("수량은 0보다 커야 합니다.");
        }
        if (this.currentStock < quantity) {
            throw new BusinessException(
                    "완제품 재고 부족: " + this.name + " (현재 " + this.currentStock + "개, 요청 " + quantity + "개)");
        }
        this.currentStock -= quantity;
    }
}
