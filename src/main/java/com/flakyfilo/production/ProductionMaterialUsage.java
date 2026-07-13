package com.flakyfilo.production;

import com.flakyfilo.material.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 생산 시점에 "정확히 이 원재료를 이만큼 썼다"를 스냅샷으로 남겨둔 것.
 * 나중에 레시피가 바뀌어도, 이 생산 건을 취소할 때는 항상 "그때 실제로 쓴 양" 그대로 복원해야
 * 정확하다 - 그래서 레시피를 다시 계산하지 않고 이 스냅샷을 그대로 참조한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_material_usage")
public class ProductionMaterialUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_log_id", nullable = false)
    private ProductionLog productionLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity_used", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityUsed;

    @Builder(access = AccessLevel.PRIVATE)
    private ProductionMaterialUsage(ProductionLog productionLog, Material material, BigDecimal quantityUsed) {
        this.productionLog = productionLog;
        this.material = material;
        this.quantityUsed = quantityUsed;
    }

    public static ProductionMaterialUsage register(ProductionLog productionLog, Material material, BigDecimal quantityUsed) {
        return ProductionMaterialUsage.builder()
                .productionLog(productionLog)
                .material(material)
                .quantityUsed(quantityUsed)
                .build();
    }
}
