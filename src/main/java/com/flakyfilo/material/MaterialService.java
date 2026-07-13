package com.flakyfilo.material;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.enums.MaterialUnit;
import com.flakyfilo.common.enums.StockReasonCode;
import com.flakyfilo.common.enums.StockTransactionType;
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
                             BigDecimal minStockThreshold, BigDecimal referenceQuantity, BigDecimal referencePrice,
                             Long supplierId, String note) {
        Supplier supplier = resolveSupplier(supplierId);
        Material material = Material.register(storeId, name, unit, minStockThreshold,
                referenceQuantity, referencePrice, supplier, note);
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
                           BigDecimal referenceQuantity, BigDecimal referencePrice,
                           Long supplierId, String note) {
        Material material = findOrThrow(materialId);
        Supplier supplier = resolveSupplier(supplierId);
        material.updateInfo(name, unit, minStockThreshold, supplier, note);
        material.updateReferencePricing(referenceQuantity, referencePrice);
    }

    /**
     * "10kg에 3만원"처럼 기준 수량/가격을 갱신 - 단가는 이 값들로부터 자동 계산됨 (Material.getUnitCost() 참고)
     */
    @Transactional
    public void updateReferencePricing(Long materialId, BigDecimal referenceQuantity, BigDecimal referencePrice) {
        Material material = findOrThrow(materialId);
        material.updateReferencePricing(referenceQuantity, referencePrice);
    }

    /**
     * 원재료 입고 / 폐기·손실 처리 (IN, OUT 전용 - "이동"이라는 의미). 보정은 applyStocktake로 분리됨.
     * 단가는 여기서 안 바뀜 - 단가는 Material의 기준수량/기준가격(register, updateReferencePricing)으로만 갱신됨.
     */
    @Transactional
    public void moveStock(Long materialId, StockTransactionType type, BigDecimal quantity,
                                    String reason, LocalDate transactionDate) {
        Material material = findOrThrow(materialId);

        StockReasonCode reasonCode = switch (type) {
            case IN -> {
                material.increaseStock(quantity);
                yield StockReasonCode.PURCHASE;
            }
            case OUT -> {
                material.decreaseStock(quantity);
                yield StockReasonCode.DISPOSAL;
            }
            case ADJUST_UP, ADJUST_DOWN -> throw new BusinessException("보정은 실사(stocktake) API를 사용하세요.");
        };

        transactionRepository.save(
                MaterialStockTransaction.register(material, type, reasonCode, quantity, reason, transactionDate));
    }

    /**
     * 재고 실사 반영. "지금 실제로 몇 개인지"(절대값)를 받아서 시스템 값과의 차이를 자동 계산한다.
     * isPeriodic: 정기 실사(월말 등)인지, 그때그때 바로잡은 수시 보정인지 - 시스템이 판단 못 하는
     * "의도"의 문제라 사용자가 직접 선택한 값을 그대로 받는다.
     */
    @Transactional
    public void applyStocktake(Long materialId, BigDecimal actualStock, String reason,
                               LocalDate transactionDate, boolean isPeriodic) {
        Material material = findOrThrow(materialId);
        BigDecimal delta = material.applyStocktake(actualStock);

        if (delta.compareTo(BigDecimal.ZERO) == 0) return;

        StockTransactionType type = delta.signum() > 0
                ? StockTransactionType.ADJUST_UP
                : StockTransactionType.ADJUST_DOWN;

        StockReasonCode reasonCode = isPeriodic
                ? StockReasonCode.PERIODIC_STOCKTAKE
                : StockReasonCode.AD_HOC_CORRECTION;

        transactionRepository.save(MaterialStockTransaction.register(
                material, type, reasonCode, delta.abs(), reason, transactionDate));
    }

    @Transactional
    public void delete(Long materialId) {
        // TODO: 이 원재료를 참조 중인 레시피가 있으면 삭제 시 DB 외래키 제약으로 실패하므로 레시피를 먼저 바꾸도록 안내 필요
        Material material = findOrThrow(materialId);
        materialRepository.delete(material);
    }

    public List<MaterialStockTransaction> getTransactionHistory(Long materialId, LocalDate from, LocalDate to) {
        return transactionRepository.findByMaterialIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                materialId, from, to);
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
