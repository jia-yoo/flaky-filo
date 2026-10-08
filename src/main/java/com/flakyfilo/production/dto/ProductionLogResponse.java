package com.flakyfilo.production.dto;

import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.enums.SourceStockType;
import com.flakyfilo.production.ProductionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductionLogResponse(
        Long id,
        Long productId,
        String productName,
        int producedQuantity,
        LocalDate productionDate,
        ProductionType productionType,
        Long sourceProductId,
        String sourceProductName,
        SourceStockType sourceStockType,
        String note,
        boolean cancelled,
        LocalDateTime createdAt
) {
    public static ProductionLogResponse from(ProductionLog log) {
        return new ProductionLogResponse(
                log.getId(),
                log.getProduct().getId(),
                log.getProduct().getName(),
                log.getProducedQuantity(),
                log.getProductionDate(),
                log.getProductionType(),
                log.getSourceProduct() != null ? log.getSourceProduct().getId() : null,
                log.getSourceProduct() != null ? log.getSourceProduct().getName() : null,
                log.getSourceStockType(),
                log.getNote(),
                log.isCancelled(),
                log.getCreatedAt()
        );
    }
}
