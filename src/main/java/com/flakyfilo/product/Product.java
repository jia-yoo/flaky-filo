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

    /**
     * "당일 안 팔려서 판매 목록에선 뺐지만, 아직 뭘로 쓸지는 안 정한" 보류 재고.
     * 마감 때 currentStock에서 여기로 옮겨두고, 나중(다음날 아침 등)에
     * 다른 완제품으로 전환하거나 폐기한다. 뭐가 될지는 그때 가서 정해도 된다.
     */
    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock = 0;

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


    /** 지금 실제로 판매 중인 메뉴인지. 일일 생산/마감 화면에는 이게 true인 것만 나열된다. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * 당일 안 팔리면 원칙적으로 폐기하는 메뉴인지 (생크림 케이크 등). false면 식빵처럼 다음날도 그대로 파는 메뉴.
     * 마감 화면에서 이 값에 따라 기본 액션(폐기 vs 이월)이 미리 선택되어 보인다 - 확정은 그때 사람이 한다.
     */
    @Column(name = "auto_dispose_if_unsold", nullable = false)
    private boolean autoDisposeIfUnsold = true;

    /**
     * 즉석주문생산 메뉴인지 (조리대에서 만들어두는 게 아니라, 주문 들어올 때 그 자리에서 만드는 메뉴).
     * true면 일일 생산/마감 사이클 대상에서 빠지고, currentStock 자체를 안 쓴다 -
     * "즉석메뉴 빠른 기록"으로 주문 들어올 때마다 그 자리에서 원재료/보류재고만 차감한다.
     */
    @Column(name = "instant_production", nullable = false)
    private boolean instantProduction = false;


    @Builder(access = AccessLevel.PRIVATE)
    private Product(Long storeId, String name, String category, int price,
                    int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio,
                    boolean active, boolean autoDisposeIfUnsold, boolean instantProduction) {
        this.storeId = storeId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.yieldCount = yieldCount;
        this.overheadRate = overheadRate;
        this.targetCostRatio = targetCostRatio;
        this.active = active;
        this.autoDisposeIfUnsold = autoDisposeIfUnsold;
        this.instantProduction = instantProduction;
    }

    public static Product register(Long storeId, String name, String category, int price,
                                   Integer yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio,
                                   Boolean active, Boolean autoDisposeIfUnsold, Boolean instantProduction) {
        Validate.notBlank(name, "완제품명");
        return Product.builder()
                .storeId(storeId)
                .name(name)
                .category(category)
                .price(price)
                .yieldCount(yieldCount != null ? yieldCount : 1)
                .overheadRate(overheadRate != null ? overheadRate : new BigDecimal("0.10"))
                .targetCostRatio(targetCostRatio != null ? targetCostRatio : new BigDecimal("0.40"))
                .active(active == null || active) // 기본값 true
                .autoDisposeIfUnsold(autoDisposeIfUnsold == null || autoDisposeIfUnsold) // 기본값 true
                .instantProduction(instantProduction != null && instantProduction) // 기본값 false
                .build();
    }

    public void updateInfo(String name, String category, int price,
                           int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio,
                           boolean active, boolean autoDisposeIfUnsold, boolean instantProduction) {
        Validate.notBlank(name, "완제품명");
        Validate.strictlyPositive(yieldCount, "나오는 개수");
        this.name = name;
        this.category = category;
        this.price = price;
        this.yieldCount = yieldCount;
        this.overheadRate = overheadRate;
        this.targetCostRatio = targetCostRatio;
        this.active = active;
        this.autoDisposeIfUnsold = autoDisposeIfUnsold;
        this.instantProduction = instantProduction;
    }

    public void increaseStock(int quantity) {
        Validate.strictlyPositive(quantity, "수량");
        this.currentStock += quantity;
    }

    public void decreaseStock(int quantity) {
        Validate.strictlyPositive(quantity, "수량");
        if (this.currentStock < quantity) {
            throw new BusinessException(
                    "완제품 재고 부족: " + this.name + " (현재 " + this.currentStock + "개, 요청 " + quantity + "개)");
        }
        this.currentStock -= quantity;
    }

    /** 마감 보류: 당일 안 팔린 걸 판매 목록에서 빼고, "보류" 상태로 옮겨둔다 (아직 뭐가 될지는 안 정함). */
    public void reserveStock(int quantity) {
        decreaseStock(quantity); // currentStock에서 빠짐 (부족하면 예외)
        this.reservedStock += quantity;
    }

    /** 마감 즉시 폐기: 보류를 거치지 않고 당일 남은 재고를 곧바로 버린다 (autoDisposeIfUnsold=true인 메뉴용). */
    public void disposeStock(int quantity) {
        decreaseStock(quantity); // currentStock에서 바로 차감, reservedStock은 안 건드림
    }

    /** 보류 재고를 다른 완제품으로 전환하거나 생산에 활용할 때 그만큼 차감 (ProductionService에서 사용). */
    public void decreaseReservedStock(int quantity) {
        Validate.strictlyPositive(quantity, "수량");
        if (this.reservedStock < quantity) {
            throw new BusinessException(
                    "보류 재고 부족: " + this.name + " (보류 중 " + this.reservedStock + "개, 요청 " + quantity + "개)");
        }
        this.reservedStock -= quantity;
    }

    /** 생산 취소 시 보류 재고를 복원할 때 사용 (ProductionService.cancel). */
    public void increaseReservedStock(int quantity) {
        Validate.strictlyPositive(quantity, "수량");
        this.reservedStock += quantity;
    }

    /** 결국 못 쓰게 된 보류 재고 폐기. */
    public void wasteReservedStock(int quantity) {
        decreaseReservedStock(quantity);
    }
}
