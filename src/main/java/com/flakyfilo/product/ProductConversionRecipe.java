package com.flakyfilo.product;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.material.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * "크로와상 보류분을 아몬드크로와상으로 전환할 때, 추가로 필요한 재료"만 담는 레시피.
 * 일반 레시피(ProductRecipe)와 달리 "차이분"만 등록하면 된다 - 원본 완제품을
 * 만드는 데 이미 쓴 베이스 재료는 다시 차감하지 않기 때문.
 * (원본,대상) 완제품 조합 하나당 이 레시피가 하나씩 존재.
 * batchQuantity 개념 없이 "완제품 1개 전환당 필요량"을 바로 받는다 - 배치 단위로
 * 여러 날 나눠 전환할 수도 있어서 배치 개념이 안 맞는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_conversion_recipe")
public class ProductConversionRecipe extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_product_id", nullable = false)
    private Product sourceProduct; // 보류 재고를 제공하는 쪽 (예: 크로와상)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id", nullable = false)
    private Product targetProduct; // 전환 결과물 (예: 아몬드크로와상)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "per_unit_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal perUnitQuantity; // 전환 완제품 1개당 추가로 필요한 이 재료의 양

    @Builder(access = AccessLevel.PRIVATE)
    private ProductConversionRecipe(Product sourceProduct, Product targetProduct, Material material,
                                    BigDecimal perUnitQuantity) {
        this.sourceProduct = sourceProduct;
        this.targetProduct = targetProduct;
        this.material = material;
        this.perUnitQuantity = perUnitQuantity;
    }

    public static ProductConversionRecipe register(Product sourceProduct, Product targetProduct,
                                                   Material material, BigDecimal perUnitQuantity) {
        return ProductConversionRecipe.builder()
                .sourceProduct(sourceProduct)
                .targetProduct(targetProduct)
                .material(material)
                .perUnitQuantity(perUnitQuantity)
                .build();
    }

    public BigDecimal calculateRequiredAmount(int producedQuantity) {
        return perUnitQuantity.multiply(BigDecimal.valueOf(producedQuantity));
    }
}