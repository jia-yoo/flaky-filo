package com.flakyfilo.closing;

import com.flakyfilo.closing.dto.ClosingActionResponse;
import com.flakyfilo.closing.dto.ClosingStockActionRequest;
import com.flakyfilo.closing.dto.DailyClosingRequest;
import com.flakyfilo.closing.dto.DailyClosingStatusResponse;
import com.flakyfilo.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-closings")
@RequiredArgsConstructor
public class DailyClosingController {

    private static final Long DEFAULT_STORE_ID = 1L;
    private final DailyClosingService dailyClosingService;
    private final ProductService productService;

    @GetMapping("/{date}")
    public DailyClosingStatusResponse getStatus(@PathVariable LocalDate date) {
        List<ClosingActionResponse> actions = dailyClosingService.getActions(date).stream()
                .map(ClosingActionResponse::from)
                .toList();
        return new DailyClosingStatusResponse(date, dailyClosingService.isClosed(DEFAULT_STORE_ID, date), actions);
    }

    @PostMapping
    public ResponseEntity<Void> close(@Valid @RequestBody DailyClosingRequest request) {
        dailyClosingService.closeDay(DEFAULT_STORE_ID, request.date());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 마감을 잘못 눌렀을 때 되돌리는 용도 (예외적으로만 사용)
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> reopen(@PathVariable LocalDate date) {
        dailyClosingService.reopenDay(DEFAULT_STORE_ID, date);
        return ResponseEntity.noContent().build();
    }

    // waste-reserved-stock은 그대로 ReserveStockRequest 유지 (마감이랑 무관한 예외 액션)

    @PostMapping("/{id}/reserve-stock")
    public ResponseEntity<Void> reserveStock(@PathVariable Long id, @Valid @RequestBody ClosingStockActionRequest request) {
        productService.reserveStock(id, request.quantity(), request.closingDate());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dispose-stock")
    public ResponseEntity<Void> disposeStock(@PathVariable Long id, @Valid @RequestBody ClosingStockActionRequest request) {
        productService.disposeStock(id, request.quantity(), request.closingDate());
        return ResponseEntity.noContent().build();
    }
}
