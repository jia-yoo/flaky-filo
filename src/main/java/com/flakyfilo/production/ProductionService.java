package com.flakyfilo.production;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.enums.StockReasonCode;
import com.flakyfilo.common.enums.StockTransactionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.material.Material;
import com.flakyfilo.material.MaterialStockTransaction;
import com.flakyfilo.material.MaterialStockTransactionRepository;
import com.flakyfilo.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final ProductConversionRecipeRepository conversionRecipeRepository;
    private final ProductionLogRepository productionLogRepository;
    private final ProductionMaterialUsageRepository usageRepository;
    private final MaterialStockTransactionRepository materialTransactionRepository;

    /**
     * 생산 등록.
     * NORMAL: 완제품 자체 레시피(BOM) 기준으로 원재료부터 차감.
     * CONVERSION: sourceProductId의 보류 재고를 차감하고, 전환 레시피(차이분)만큼만 원재료 추가 차감.
     */
    @Transactional
    public ProductionLog register(Long productId, int producedQuantity, LocalDate productionDate,
                                  ProductionType productionType, Long sourceProductId, String note) {
        Product product = EntityFinder.findOrThrow(productRepository, productId, "완제품");
        Product sourceProduct = sourceProductId != null
                ? EntityFinder.findOrThrow(productRepository, sourceProductId, "원본 완제품")
                : null;

        ProductionLog log = productionLogRepository.save(
                ProductionLog.register(product, producedQuantity, productionDate, productionType, sourceProduct, note));

        if (productionType == ProductionType.CONVERSION) {
            sourceProduct.decreaseReservedStock(producedQuantity); // 보류 재고 부족하면 여기서 예외

            List<ProductConversionRecipe> conversionItems =
                    conversionRecipeRepository.findBySourceAndTarget(sourceProductId, productId);
            if (conversionItems.isEmpty()) {
                throw new BusinessException("등록된 전환 레시피가 없습니다. 먼저 전환 레시피를 등록하세요.");
            }
            for (ProductConversionRecipe item : conversionItems) {
                deductMaterialAndSnapshot(log, item.getMaterial(),
                        item.calculateRequiredAmount(producedQuantity), product, producedQuantity, productionDate);
            }
        } else {
            List<ProductRecipe> recipe = recipeRepository.findByProductId(productId);
            if (recipe.isEmpty()) {
                throw new BusinessException("레시피가 등록되지 않아 생산할 수 없습니다. 먼저 레시피를 등록하세요.");
            }
            for (ProductRecipe item : recipe) {
                BigDecimal requiredAmount = item.calculatePerUnitQuantity().multiply(BigDecimal.valueOf(producedQuantity));
                deductMaterialAndSnapshot(log, item.getMaterial(), requiredAmount, product, producedQuantity, productionDate);
            }
        }

        product.increaseStock(producedQuantity);
        return log;
    }

    private void deductMaterialAndSnapshot(ProductionLog log, Material material, BigDecimal requiredAmount,
                                           Product product, int producedQuantity, LocalDate productionDate) {
        material.decreaseStock(requiredAmount); // 부족하면 여기서 예외 -> 전체 롤백

        materialTransactionRepository.save(MaterialStockTransaction.register(
                material, StockTransactionType.OUT, StockReasonCode.PRODUCTION_CONSUMPTION,
                requiredAmount, "생산: " + product.getName() + " " + producedQuantity + "개", productionDate));

        usageRepository.save(ProductionMaterialUsage.register(log, material, requiredAmount));
    }

    /**
     * 생산 취소. 완제품 재고 원상복구, 원재료는 스냅샷 기준 정확히 복원,
     * CONVERSION이었으면 원본 완제품의 보류 재고도 복원한다.
     */
    @Transactional
    public void cancel(Long productionLogId) {
        ProductionLog log = productionLogRepository.findByIdWithProducts(productionLogId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 생산 기록입니다. id=" + productionLogId));
        log.cancel();

        Product product = log.getProduct();
        product.decreaseStock(log.getProducedQuantity());

        if (log.getProductionType() == ProductionType.CONVERSION) {
            log.getSourceProduct().increaseReservedStock(log.getProducedQuantity());
        }

        List<ProductionMaterialUsage> usages = usageRepository.findByProductionLogId(productionLogId);
        LocalDate today = LocalDate.now();

        for (ProductionMaterialUsage usage : usages) {
            Material material = usage.getMaterial();
            material.increaseStock(usage.getQuantityUsed());

            materialTransactionRepository.save(MaterialStockTransaction.register(
                    material, StockTransactionType.IN, StockReasonCode.PRODUCTION_CONSUMPTION,
                    usage.getQuantityUsed(), "생산 취소로 인한 원재료 복원 (생산기록 #" + productionLogId + ")", today));
        }
    }

    public List<ProductionLog> getHistory(Long productId) {
        return productionLogRepository.findByProductId(productId);
    }
}
