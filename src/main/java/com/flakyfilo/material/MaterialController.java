package com.flakyfilo.material;

import com.flakyfilo.material.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    //TODO: 나중에 로그인 붙으면 로그인한 사용자의 storeId를 꺼내 쓰도록 바뀔 지점.
    private static final Long DEFAULT_STORE_ID = 1L; // MVP 단계라 매장이 하나뿐이라 storeId를 일단 고정값으로 둔다.
    private final MaterialService materialService;

    @PostMapping
    public ResponseEntity<MaterialResponse> create(@Valid @RequestBody MaterialCreateRequest request) {
        Material material = materialService.register(
                request.storeId(), request.name(), request.unit(),
                request.minStockThreshold(), request.unitCost(),
                request.supplierId(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(MaterialResponse.from(material));
    }

    @GetMapping
    public List<MaterialResponse> getAll() {
        return materialService.getAll(DEFAULT_STORE_ID).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MaterialResponse getById(@PathVariable Long id) {
        return MaterialResponse.from(materialService.getById(id));
    }

    @GetMapping("/low-stock")
    public List<MaterialResponse> getLowStock() {
        return materialService.getLowStock(DEFAULT_STORE_ID).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @GetMapping("/{id}/transactions")
    public List<MaterialStockTransactionResponse> getTransactionHistory(@PathVariable Long id) {
        return materialService.getTransactionHistory(id).stream()
                .map(MaterialStockTransactionResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialUpdateRequest request) {
        materialService.updateInfo(id, request.name(), request.unit(), request.minStockThreshold(),
                request.supplierId(), request.note());
        return MaterialResponse.from(materialService.getById(id));
    }

    /**
     * 재고 실사 반영. adjustStock(POST)과 달리 PUT을 쓰는 이유:
     * "지금 실제로 N개다"라는 절대값을 전달하는 거라, 같은 요청을 몇 번 반복해도
     * 최종 재고는 항상 N으로 고정됨 (멱등) -> PUT이 맞음.
     * 반면 adjustStock은 "5개를 추가로 입고했다"는 변화량이라, 반복하면 결과가 계속 바뀜 -> POST.
     */
    @PutMapping("/{id}/stocktake")
    public MaterialResponse applyStocktake(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        materialService.applyStocktake(id, request.quantity(), request.reason(), request.transactionDate());
        return MaterialResponse.from(materialService.getById(id));
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<Void> adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        materialService.adjustStock(id, request.type(), request.quantity(), request.reason(), request.transactionDate());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
