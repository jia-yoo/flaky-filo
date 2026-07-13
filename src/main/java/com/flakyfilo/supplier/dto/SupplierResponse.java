package com.flakyfilo.supplier.dto;

import com.flakyfilo.supplier.Supplier;

public record SupplierResponse(
        Long id,
        String name,
        String note
) {
    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getNote());
    }
}
