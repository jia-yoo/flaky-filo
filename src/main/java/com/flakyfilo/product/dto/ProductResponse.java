package com.flakyfilo.product.dto;

import com.flakyfilo.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        Long storeId,
        String name,
        String category,
        int price,
        int currentStock,
        int reservedStock,
        int yieldCount,
        BigDecimal overheadRate,
        BigDecimal targetCostRatio,
        boolean active,
        boolean autoDisposeIfUnsold,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getStoreId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getCurrentStock(),
                product.getReservedStock(),
                product.getYieldCount(),
                product.getOverheadRate(),
                product.getTargetCostRatio(),
                product.isActive(),
                product.isAutoDisposeIfUnsold(),
                product.getCreatedAt()
        );
    }
}
