package com.flakyfilo.production;

import com.flakyfilo.production.dto.ProductionLogResponse;
import com.flakyfilo.production.dto.ProductionRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @PostMapping
    public ResponseEntity<ProductionLogResponse> register(@Valid @RequestBody ProductionRegisterRequest request) {
        ProductionLog log = productionService.register(
                request.productId(), request.producedQuantity(), request.productionDate(),
                request.productionType(), request.sourceProductId(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductionLogResponse.from(log));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        productionService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/instant")
    public ResponseEntity<ProductionLogResponse> registerInstant(@Valid @RequestBody ProductionRegisterRequest request) {
        ProductionLog log = productionService.registerInstant(
                request.productId(), request.producedQuantity(), request.productionDate(),
                request.productionType(), request.sourceProductId(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductionLogResponse.from(log));
    }

    @GetMapping
    public List<ProductionLogResponse> getHistory(@RequestParam Long productId) {
        return productionService.getHistory(productId).stream().map(ProductionLogResponse::from).toList();
    }
}
