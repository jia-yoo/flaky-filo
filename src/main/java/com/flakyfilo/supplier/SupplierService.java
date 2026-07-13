package com.flakyfilo.supplier;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.material.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public Supplier register(String name, String note) {
        return supplierRepository.save(Supplier.register(name, note));
    }

    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = EntityFinder.findOrThrow(supplierRepository, id, "구매처");

        // DB 외래키 제약이 막아주긴 하지만, 그러면 정제 안 된 에러가 나가니 여기서 미리 확인해서 사용자에게 이해되는 메시지로 안내한다.
        if (materialRepository.existsBySupplierId(id)) {
            throw new BusinessException(
                    "이 구매처를 사용 중인 원재료가 있어 삭제할 수 없습니다. 먼저 해당 원재료의 구매처를 변경해주세요.");
        }

        supplierRepository.delete(supplier);
    }
}
