package com.flakyfilo.product;

import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.product.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Long DEFAULT_STORE_ID = 1L;
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.register(
                request.storeId(), request.name(), request.category(), request.price(),
                request.yieldCount(), request.overheadRate(), request.targetCostRatio(),
                request.active(), request.autoDisposeIfUnsold(), request.instantProduction());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAll(DEFAULT_STORE_ID).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        productService.updateInfo(id, request.name(), request.category(), request.price(),
                request.yieldCount(), request.overheadRate(), request.targetCostRatio(),
                request.active(), request.autoDisposeIfUnsold(), request.instantProduction());
        return ProductResponse.from(productService.getById(id));
    }

    // 일일 생산/마감 화면 전용 - 판매중인 것만
    @GetMapping("/active")
    public List<ProductResponse> getActiveProducts() {
        return productService.getActiveProducts(DEFAULT_STORE_ID).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/active/scheduled")
    public List<ProductResponse> getScheduledProductionProducts() {
        return productService.getScheduledProductionProducts(DEFAULT_STORE_ID).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/active/instant")
    public List<ProductResponse> getInstantProductionProducts() {
        return productService.getInstantProductionProducts(DEFAULT_STORE_ID).stream().map(ProductResponse::from).toList();
    }

    // 마감 즉시 폐기 (기존 ReserveStockRequest 재사용 - quantity 하나만 있으면 됨)
    @PostMapping("/{id}/dispose-stock")
    public ResponseEntity<Void> disposeStock(@PathVariable Long id, @Valid @RequestBody ReserveStockRequest request) {
        productService.disposeStock(id, request.quantity(), request.closingDate());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recipe")
    public List<RecipeItemResponse> getRecipe(@PathVariable Long id) {
        return productService.getRecipe(id).stream().map(RecipeItemResponse::from).toList();
    }

    @PutMapping("/{id}/recipe")
    public List<RecipeItemResponse> updateRecipe(@PathVariable Long id, @Valid @RequestBody RecipeUpdateRequest request) {
        List<ProductService.RecipeItem> items = request.items().stream()
                .map(item -> new ProductService.RecipeItem(item.materialId(), item.batchQuantity()))
                .toList();
        productService.setRecipe(id, items);
        return productService.getRecipe(id).stream().map(RecipeItemResponse::from).toList();
    }

    @GetMapping("/{targetId}/conversion-recipes")
    public List<ConversionRecipeItemResponse> getConversionRecipesByTarget(@PathVariable Long targetId) {
        return productService.getConversionRecipesByTarget(targetId).stream()
                .map(ConversionRecipeItemResponse::from).toList();
    }

    @GetMapping("/{sourceId}/conversion-recipe/{targetId}")
    public List<ConversionRecipeItemResponse> getConversionRecipe(@PathVariable Long sourceId, @PathVariable Long targetId) {
        return productService.getConversionRecipe(sourceId, targetId).stream()
                .map(ConversionRecipeItemResponse::from).toList();
    }

    @PutMapping("/{sourceId}/conversion-recipe/{targetId}")
    public List<ConversionRecipeItemResponse> updateConversionRecipe(
            @PathVariable Long sourceId, @PathVariable Long targetId,
            @Valid @RequestBody ConversionRecipeUpdateRequest request) {
        List<ProductService.RecipeItem> items = request.items().stream()
                .map(item -> new ProductService.RecipeItem(item.materialId(), item.perUnitQuantity()))
                .toList();
        productService.setConversionRecipe(sourceId, targetId, items);
        return productService.getConversionRecipe(sourceId, targetId).stream()
                .map(ConversionRecipeItemResponse::from).toList();
    }

    @PostMapping("/{id}/reserve-stock")
    public ResponseEntity<Void> reserveStock(@PathVariable Long id, @Valid @RequestBody ReserveStockRequest request) {
        productService.reserveStock(id, request.quantity(), request.closingDate());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/waste-reserved-stock")
    public ResponseEntity<Void> wasteReservedStock(@PathVariable Long id, @Valid @RequestBody ReserveStockRequest request) {
        productService.wasteReservedStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cost")
    public CostResponse getCost(@PathVariable Long id) {
        return CostResponse.from(productService.calculateCost(id));
    }

    @GetMapping("/{id}/channel-prices")
    public List<ChannelPriceResponse> getChannelPrices(@PathVariable Long id) {
        return productService.getChannelPrices(id).stream().map(ChannelPriceResponse::from).toList();
    }

    @PutMapping("/{id}/channel-prices/{channel}")
    public ResponseEntity<Void> setChannelPrice(@PathVariable Long id, @PathVariable OrderChannel channel,
                                                @Valid @RequestBody ChannelPriceRequest request) {
        productService.setChannelPrice(id, channel, request.price());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/channel-prices/{channel}")
    public ResponseEntity<Void> removeChannelPrice(@PathVariable Long id, @PathVariable OrderChannel channel) {
        productService.removeChannelPrice(id, channel);
        return ResponseEntity.noContent().build();
    }
}
