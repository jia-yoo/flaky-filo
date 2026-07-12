package com.flakyfilo.supplier;

import com.flakyfilo.common.EntityFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional
    public Supplier register(String name, String phone, String note) {
        return supplierRepository.save(Supplier.register(name, phone, note));
    }

    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = EntityFinder.findOrThrow(supplierRepository, id, "구매처");
        // TODO: 이 구매처를 참조 중인 원재료가 있으면 삭제 시 DB 외래키 제약으로 실패함 (의도된 동작 -
        // 참조 무결성이 지켜져야 하니, 삭제 전에 원재료 쪽 구매처를 먼저 바꾸도록 안내 필요)
        supplierRepository.delete(supplier);
    }
}
