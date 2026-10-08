package com.flakyfilo.order;

import com.flakyfilo.order.dto.OrderCreateRequest;
import com.flakyfilo.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        List<OrderService.OrderLine> lines = request.items().stream()
                .map(item -> new OrderService.OrderLine(
                        item.productId(),
                        item.quantity(),
                        item.substitutions() == null ? List.of() : item.substitutions().stream()
                                .map(s -> new OrderService.SubstitutionLine(s.substituteProductId(), s.quantity()))
                                .toList()
                ))
                .toList();
        Order order = orderService.register(request.channel(), request.orderDate(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return OrderResponse.from(orderService.getById(id));
    }

    // 기간(from~to)으로 주문 목록 조회 - 이력이 계속 쌓이는 데이터라 처음부터 기간 필터링 (Material 이력과 동일한 원칙)
    @GetMapping
    public List<OrderResponse> getByDateRange(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return orderService.getByDateRange(from, to).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
