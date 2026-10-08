package com.flakyfilo.product;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.OrderChannel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채널(네이버/쿠팡/배민 등)마다 다른 판매가를 쓸 때만 등록하는 "예외" 가격표.
 * 이 테이블에 값이 없는 채널은 그냥 Product.price(매장 기본가)를 그대로 쓴다 -
 * 그래서 채널이 늘어나도 Product에 컬럼을 계속 추가할 필요가 없다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_channel_price",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "channel"}))
public class ProductChannelPrice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private OrderChannel channel;

    @Column(nullable = false)
    private int price;

    @Builder(access = AccessLevel.PRIVATE)
    private ProductChannelPrice(Product product, OrderChannel channel, int price) {
        this.product = product;
        this.channel = channel;
        this.price = price;
    }

    public static ProductChannelPrice register(Product product, OrderChannel channel, int price) {
        Validate.notNegative(price, "채널별 가격");
        return ProductChannelPrice.builder().product(product).channel(channel).price(price).build();
    }

    public void updatePrice(int price) {
        Validate.notNegative(price, "채널별 가격");
        this.price = price;
    }
}
