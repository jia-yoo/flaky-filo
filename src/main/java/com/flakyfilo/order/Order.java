package com.flakyfilo.order;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문. 상태값(접수/준비중/완료) 개념 없이, 등록 = 즉시 판매확정으로 취급한다.
 * OrderItem은 Order 없이는 존재 의미가 없는 종속 데이터라 OneToMany + cascade로 함께 관리한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderChannel channel;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount = 0;

    @Column(nullable = false)
    private boolean cancelled = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Order(OrderChannel channel, LocalDate orderDate) {
        this.orderNumber = generateOrderNumber();
        this.channel = channel;
        this.orderDate = orderDate != null ? orderDate : LocalDate.now();
    }

    public static Order register(OrderChannel channel, LocalDate orderDate) {
        if (channel == null) {
            throw new BusinessException("주문 채널은 필수입니다.");
        }
        return Order.builder().channel(channel).orderDate(orderDate).build();
    }

    /**
     * 주문 항목 추가.
     * unitPrice는 호출하는 쪽(OrderService)이 채널별 가격을 확인해서 넘겨준다.
     * stockAffected는 호출하는 쪽이 "이 항목이 재고에 실제로 영향을 줘야 하는지"(즉석주문생산이거나
     * 이미 마감된 날짜면 false) 판단해서 넘겨준다 - Order/OrderItem은 그 판단 기준 자체를 몰라도 된다.
     * substitutions(대체발송)이 있으면, 대체분은 각각의 대체 완제품 재고에서, 나머지는 원래 완제품 재고에서 차감한다.
     */
    public void addItem(Product product, int quantity, int unitPrice, boolean stockAffected,
                         List<SubstitutionInput> substitutions) {
        if (this.cancelled) {
            throw new BusinessException("취소된 주문은 수정할 수 없습니다.");
        }

        int substitutedTotal = substitutions.stream().mapToInt(SubstitutionInput::quantity).sum();
        if (substitutedTotal > quantity) {
            throw new BusinessException("대체발송 수량 합계가 주문 수량보다 많을 수 없습니다.");
        }

        if (stockAffected) {
            int mainQuantity = quantity - substitutedTotal;
            if (mainQuantity > 0) {
                product.decreaseStock(mainQuantity);
            }
            for (SubstitutionInput sub : substitutions) {
                sub.substituteProduct().decreaseStock(sub.quantity());
            }
        }

        OrderItem item = OrderItem.register(this, product, quantity, unitPrice, stockAffected);
        for (SubstitutionInput sub : substitutions) {
            item.addSubstitution(sub.substituteProduct(), sub.quantity());
        }

        this.items.add(item);
        this.totalAmount += item.getSubtotal();
    }

    /** 주문 취소. 과거 기록을 고치지 않고 cancelled 플래그만 세운다 (재고 복원은 OrderService가 처리). */
    public void cancel() {
        if (this.cancelled) {
            throw new BusinessException("이미 취소된 주문입니다.");
        }
        this.cancelled = true;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record SubstitutionInput(Product substituteProduct, int quantity) {
    }
}
