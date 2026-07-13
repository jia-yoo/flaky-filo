package com.flakyfilo.product;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.material.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 완제품 레시피의 한 줄. 소요량은 "완제품 1개 기준"이 아니라
 * "이 레시피 한 번(배치) 전체를 만들 때 필요한 양"으로 입력받는다
 * (실제 레시피 그대로 입력하기 편하도록 - 예: "밀가루 390g" 전체 배치 기준).
 * 개당 원가는 Product.yieldCount(나오는 개수)로 나눠서 계산한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_recipe")
public class ProductRecipe extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "batch_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal batchQuantity; // 배치 전체 기준 필요량 (예: 390g)

    @Builder(access = AccessLevel.PRIVATE)
    private ProductRecipe(Product product, Material material, BigDecimal batchQuantity) {
        this.product = product;
        this.material = material;
        this.batchQuantity = batchQuantity;
    }

    public static ProductRecipe register(Product product, Material material, BigDecimal batchQuantity) {
        return ProductRecipe.builder()
                .product(product)
                .material(material)
                .batchQuantity(batchQuantity)
                .build();
    }

    /** 완제품 1개당 소요량 = 배치 전체 필요량 ÷ 나오는 개수 */
    public BigDecimal calculatePerUnitQuantity() {
        return batchQuantity.divide(BigDecimal.valueOf(product.getYieldCount()), 4, RoundingMode.HALF_UP);
    }

    /** 이 레시피 항목 하나가 "완제품 1개당" 원가에 기여하는 금액 */
    public BigDecimal calculateCostContribution() {
        return calculatePerUnitQuantity().multiply(material.getUnitCost());
    }
}
