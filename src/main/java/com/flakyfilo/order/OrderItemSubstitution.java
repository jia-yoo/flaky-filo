package com.flakyfilo.order;

import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 한 줄(OrderItem) 안에서 "원래 시킨 완제품 대신 다른 완제품으로 보낸" 수량 한 건.
 * 하루 합계 단위 주문이라 여러 손님에게 있었던 대체발송을 다 합친 "그날의 대체 합계"로 기록한다
 * (건별 추적은 아님 - 재고 정확도는 어차피 마감 실사가 책임지고, 이건 매출/생산계획 참고용 통계).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_item_substitution")
public class OrderItemSubstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_product_id", nullable = false)
    private Product substituteProduct;

    @Column(nullable = false)
    private int quantity;

    @Builder(access = AccessLevel.PRIVATE)
    private OrderItemSubstitution(OrderItem orderItem, Product substituteProduct, int quantity) {
        this.orderItem = orderItem;
        this.substituteProduct = substituteProduct;
        this.quantity = quantity;
    }

    static OrderItemSubstitution register(OrderItem orderItem, Product substituteProduct, int quantity) {
        return OrderItemSubstitution.builder()
                .orderItem(orderItem).substituteProduct(substituteProduct).quantity(quantity).build();
    }
}
