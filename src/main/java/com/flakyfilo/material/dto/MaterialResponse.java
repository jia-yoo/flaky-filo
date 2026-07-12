package com.flakyfilo.material.dto;

import com.flakyfilo.common.MaterialUnit;
import com.flakyfilo.material.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        Long storeId,
        String name,
        MaterialUnit unit,
        BigDecimal currentStock,
        BigDecimal minStockThreshold,
        BigDecimal unitCost,
        Long supplierId,
        String supplierName, // 화면에 바로 표시할 수 있게 이름까지 내려줌 (프론트가 또 조회 안 해도 되게)
        String note,
        boolean belowThreshold,
        LocalDateTime createdAt
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getStoreId(),
                material.getName(),
                material.getUnit(),
                material.getCurrentStock(),
                material.getMinStockThreshold(),
                material.getUnitCost(),
                material.getSupplier() != null ? material.getSupplier().getId() : null,
                material.getSupplier() != null ? material.getSupplier().getName() : null,
                material.getNote(),
                material.isBelowThreshold(),
                material.getCreatedAt()
        );
    }
}