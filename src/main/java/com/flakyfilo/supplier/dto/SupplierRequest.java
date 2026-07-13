package com.flakyfilo.supplier.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String name,
        String note
) {
}
