package com.flakyfilo.material;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.MaterialUnit;
import com.flakyfilo.common.StockTransactionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.supplier.Supplier;
import com.flakyfilo.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 원재료 관련 비즈니스 로직.
 * 재고는 반드시 이 서비스를 거쳐서만 변경되고, 변경할 때마다
 * MaterialStockTransaction 이력이 함께 남는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 조회 전용. 쓰기 메서드에만 개별로 @Transactional 재선언
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialStockTransactionRepository transactionRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public Material register(Long storeId, String name, MaterialUnit unit,
                             BigDecimal minStockThreshold, BigDecimal unitCost,
                             Long supplierId, String note) {
        Supplier supplier = resolveSupplier(supplierId);
        Material material = Material.register(storeId, name, unit, minStockThreshold, unitCost, supplier, note); // ← 여기서 진짜 Supplier 객체를 넘김
        return materialRepository.save(material);
    }
    public Material getById(Long materialId) {
        return findOrThrow(materialId);
    }

    public List<Material> getAll(Long storeId) {
        return materialRepository.findByStoreId(storeId);
    }

    public List<Material> getLowStock(Long storeId) {
        return materialRepository.findLowStock(storeId);
    }

    @Transactional
    public void updateInfo(Long materialId, String name, MaterialUnit unit, BigDecimal minStockThreshold,
                           Long supplierId, String note) {
        Material material = findOrThrow(materialId);
        Supplier supplier = resolveSupplier(supplierId);
        material.updateInfo(name, unit, minStockThreshold, supplier, note);
        // JPA 영속성 컨텍스트 안에서 필드만 바꾸면, 트랜잭션 커밋 시점에 자동으로 UPDATE 쿼리가 나간다.
        // (= "더티 체킹". materialRepository.save()를 따로 안 불러도 됨)
    }

    @Transactional
    public void updateUnitCost(Long materialId, BigDecimal newUnitCost) {
        Material material = findOrThrow(materialId);
        material.updateUnitCost(newUnitCost);
    }

    /**
     * 원재료 입고 / 폐기·손실 처리 (IN, OUT만 - 보정은 applyStocktake로 분리됨).
     */
    @Transactional
    public void adjustStock(Long materialId, StockTransactionType type, BigDecimal quantity,
                            String reason, LocalDate transactionDate) {
        Material material = findOrThrow(materialId);

        switch (type) {
            case IN -> material.increaseStock(quantity);
            case OUT -> material.decreaseStock(quantity);
        }

        transactionRepository.save(MaterialStockTransaction.register(material, type, quantity, reason, transactionDate));
    }

    /**
     * 재고 실사 반영. "지금 실제로 몇 개인지"(절대값)를 받아서 시스템 값과의 차이를 자동 계산한다.
     */
    @Transactional
    public void applyStocktake(Long materialId, BigDecimal actualStock, String reason, LocalDate transactionDate) {
        Material material = findOrThrow(materialId);
        BigDecimal delta = material.applyStocktake(actualStock);

        if (delta.compareTo(BigDecimal.ZERO) == 0) return; // 변동 없으면 이력 안 남김

        StockTransactionType type = delta.signum() > 0 ? StockTransactionType.ADJUST_UP : StockTransactionType.ADJUST_DOWN;
        transactionRepository.save(MaterialStockTransaction.register(material, type, delta.abs(), reason, transactionDate));
    }

    @Transactional
    public void delete(Long materialId) {
        // TODO: 이 원재료를 참조 중인 레시피가 있으면 삭제 시 DB 외래키 제약으로 실패하므로 레시피를 먼저 바꾸도록 안내 필요
        Material material = findOrThrow(materialId);
        materialRepository.delete(material);
    }

    public List<MaterialStockTransaction> getTransactionHistory(Long materialId) {
        return transactionRepository.findByMaterialIdOrderByCreatedAtDesc(materialId);
    }

    // supplierId가 null이면 "아직 구매처 모름"으로 허용, 값이 있으면 실존 여부 확인
    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) return null;
        return EntityFinder.findOrThrow(supplierRepository, supplierId, "구매처");
    }

    private Material findOrThrow(Long materialId) {
        return materialRepository.findByIdWithSupplier(materialId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 원재료입니다. id=" + materialId));
    }
}
