package com.flakyfilo.order;

import com.flakyfilo.closing.DailyClosingService;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.product.Product;
import com.flakyfilo.product.ProductChannelPriceRepository;
import com.flakyfilo.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final Long DEFAULT_STORE_ID = 1L;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductChannelPriceRepository channelPriceRepository;
    private final DailyClosingService closingService;

    /**
     * 주문 생성 - 상태값 없이 등록 즉시 판매 확정.
     * 재고 영향 여부는 항목마다 다르게 결정된다:
     *   - 즉석주문생산 메뉴면 항상 재고 영향 없음 (즉석메뉴 빠른 기록으로 이미 별도 처리됨)
     *   - 그 외에는 주문 날짜가 이미 마감됐으면 재고 영향 없음(순수 매출 기록), 마감 전이면 실시간 차감
     * 대체발송이 있으면 재고 영향이 있는 경우에 한해 대체 완제품 쪽에서 차감된다.
     */
    @Transactional
    public Order register(OrderChannel channel, LocalDate orderDate, List<OrderLine> lines) {
        Validate.notEmpty(lines, "주문 항목");

        Order order = Order.register(channel, orderDate);
        boolean dateClosed = closingService.isClosed(DEFAULT_STORE_ID, order.getOrderDate());

        for (OrderLine line : lines) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new BusinessException("존재하지 않는 완제품입니다. id=" + line.productId()));

            boolean stockAffected = !product.isInstantProduction() && !dateClosed;
            int unitPrice = resolvePrice(product, channel);

            List<Order.SubstitutionInput> substitutions = new ArrayList<>();
            for (SubstitutionLine sub : line.substitutions()) {
                Product substituteProduct = productRepository.findById(sub.substituteProductId())
                        .orElseThrow(() -> new BusinessException("존재하지 않는 완제품입니다. id=" + sub.substituteProductId()));
                substitutions.add(new Order.SubstitutionInput(substituteProduct, sub.quantity()));
            }

            order.addItem(product, line.quantity(), unitPrice, stockAffected, substitutions);
        }

        return orderRepository.save(order);
    }

    private int resolvePrice(Product product, OrderChannel channel) {
        return channelPriceRepository.findByProductIdAndChannel(product.getId(), channel)
                .map(cp -> cp.getPrice())
                .orElse(product.getPrice());
    }

    public Order getById(Long orderId) {
        Order order = findOrThrow(orderId);
        initializeSubstitutions(order);
        return order;
    }

    public List<Order> getByDateRange(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByOrderDateBetween(from, to);
        orders.forEach(this::initializeSubstitutions);
        return orders;
    }

    // items(bag)와 substitutions(bag)를 한 쿼리에서 같이 fetch join 하면 Hibernate가
    // "두 개의 bag 컬렉션을 동시에 fetch 못 한다"고 예외를 던진다(MultipleBagFetchException).
    // 그래서 items는 fetch join으로, substitutions는 트랜잭션 안에서 지연로딩을 그냥 터치해서 초기화한다
    // (주문 하나당 항목 몇 개, 대체발송 몇 개 수준이라 N+1이어도 이 규모에선 문제없음).
    private void initializeSubstitutions(Order order) {
        for (OrderItem item : order.getItems()) {
            for (OrderItemSubstitution sub : item.getSubstitutions()) {
                sub.getSubstituteProduct().getName(); // 강제로 초기화
            }
        }
    }

    /**
     * 주문 취소 - 과거 기록은 그대로 두고 cancelled 플래그만 세운 뒤, 재고 영향이 있었던 항목만 복원한다.
     * 마감 이후에는 재고 복원을 하면 안 된다 (마감 실사로 이미 확정된 숫자를 건드리게 되므로) -
     * stockAffected=true인 항목이 있는데 지금 그 날짜가 마감돼 있으면 취소 자체를 막는다.
     */
    @Transactional
    public void cancel(Long orderId) {
        Order order = findOrThrow(orderId);
        boolean hasStockAffectedItem = order.getItems().stream().anyMatch(OrderItem::isStockAffected);

        if (hasStockAffectedItem && closingService.isClosed(DEFAULT_STORE_ID, order.getOrderDate())) {
            throw new BusinessException("이미 마감된 날짜의 주문이라 재고에 영향을 준 항목은 취소할 수 없습니다.");
        }

        order.cancel();

        for (OrderItem item : order.getItems()) {
            if (!item.isStockAffected()) continue;

            int substitutedTotal = item.getSubstitutions().stream()
                    .mapToInt(OrderItemSubstitution::getQuantity).sum();
            int mainQuantity = item.getQuantity() - substitutedTotal;

            if (mainQuantity > 0) {
                item.getProduct().increaseStock(mainQuantity);
            }
            for (OrderItemSubstitution sub : item.getSubstitutions()) {
                sub.getSubstituteProduct().increaseStock(sub.getQuantity());
            }
        }
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 주문입니다. id=" + orderId));
    }

    public record OrderLine(Long productId, int quantity, List<SubstitutionLine> substitutions) {
    }

    public record SubstitutionLine(Long substituteProductId, int quantity) {
    }
}
