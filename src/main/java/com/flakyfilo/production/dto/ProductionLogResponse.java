package com.flakyfilo.production.dto;

import com.flakyfilo.production.ProductionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductionLogResponse(
        Long id,
        Long productId,
        String productName,
        int producedQuantity,
        LocalDate productionDate,
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
                log.getNote(),
                log.isCancelled(),
                log.getCreatedAt()
        );
    }
}
