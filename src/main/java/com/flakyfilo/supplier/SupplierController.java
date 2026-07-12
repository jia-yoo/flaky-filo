package com.flakyfilo.supplier;

import com.flakyfilo.supplier.dto.SupplierRequest;
import com.flakyfilo.supplier.dto.SupplierResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.register(request.name(), request.phone(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(SupplierResponse.from(supplier));
    }

    @GetMapping
    public List<SupplierResponse> getAll() {
        return supplierService.getAll().stream().map(SupplierResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
