package com.flakyfilo.order;

import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 한 줄. unitPrice는 "주문 당시(그리고 그 채널 기준) 판매가"를 스냅샷으로 저장한다.
 * quantity는 "원래 주문받은 수량"(매출 계산 기준) - 대체발송이 있어도 이 값은 안 바뀐다.
 * stockAffected는 이 항목이 생성될 때 실제로 재고를 건드렸는지 기록해둔 것 -
 * 주문 취소 시 이 값을 보고 재고를 복원해야 하는지 판단한다 (즉석주문생산이거나 마감된 날짜였으면 false).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int subtotal;

    @Column(name = "stock_affected", nullable = false)
    private boolean stockAffected;

    // 대체발송 - 하루 합계 단위 주문 안에서, 원래 완제품 대신 다른 완제품으로 나간 수량들 (여러 개 가능)
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemSubstitution> substitutions = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private OrderItem(Order order, Product product, int quantity, int unitPrice, boolean stockAffected) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * quantity;
        this.stockAffected = stockAffected;
    }

    static OrderItem register(Order order, Product product, int quantity, int unitPrice, boolean stockAffected) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .stockAffected(stockAffected)
                .build();
    }

    void addSubstitution(Product substituteProduct, int quantity) {
        this.substitutions.add(OrderItemSubstitution.register(this, substituteProduct, quantity));
    }
}
