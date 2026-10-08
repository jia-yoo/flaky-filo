package com.flakyfilo.production;

import com.flakyfilo.closing.DailyClosingService;
import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.ProductionType;
import com.flakyfilo.common.enums.SourceStockType;
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

    private static final Long DEFAULT_STORE_ID = 1L;
    private final ProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final ProductConversionRecipeRepository conversionRecipeRepository;
    private final ProductionLogRepository productionLogRepository;
    private final ProductionMaterialUsageRepository usageRepository;
    private final MaterialStockTransactionRepository materialTransactionRepository;
    private final DailyClosingService closingService;

    /**
     * 일반 생산 등록 (일일 생산/마감 화면 전용). 마감된 날짜면 막힌다.
     * CONVERSION이면 sourceStockType으로 원본의 보류재고를 쓸지 당일생산분(현재고)을 쓸지 선택한다.
     */
    @Transactional
    public ProductionLog register(Long productId, int producedQuantity, LocalDate productionDate,
                                   ProductionType productionType, Long sourceProductId,
                                   SourceStockType sourceStockType, String note) {
        LocalDate effectiveDate = productionDate != null ? productionDate : LocalDate.now();
        closingService.assertNotClosed(DEFAULT_STORE_ID, effectiveDate);

        Product product = EntityFinder.findOrThrow(productRepository, productId, "완제품");
        Product sourceProduct = resolveSourceProduct(sourceProductId);

        ProductionLog log = productionLogRepository.save(
                ProductionLog.register(product, producedQuantity, productionDate, productionType,
                        sourceProduct, sourceStockType, note));

        processMaterialDeduction(log, product, sourceProduct, productionType, sourceProductId,
                sourceStockType, producedQuantity, productionDate);

        product.increaseStock(producedQuantity); // 즉석 기록과의 유일한 차이 - 완제품 재고를 실제로 늘림
        return log;
    }

    /**
     * 즉석메뉴 빠른 기록 전용. 완제품 재고(currentStock)를 늘리지 않는다.
     * 마감 사이클과 무관해서 마감 여부도 확인하지 않는다.
     */
    @Transactional
    public ProductionLog registerInstant(Long productId, int producedQuantity, LocalDate productionDate,
                                          ProductionType productionType, Long sourceProductId,
                                          SourceStockType sourceStockType, String note) {
        Product product = EntityFinder.findOrThrow(productRepository, productId, "완제품");
        if (!product.isInstantProduction()) {
            throw new BusinessException(product.getName() + "은(는) 즉석주문생산 메뉴가 아닙니다.");
        }
        Product sourceProduct = resolveSourceProduct(sourceProductId);

        ProductionLog log = productionLogRepository.save(
                ProductionLog.registerInstant(product, producedQuantity, productionDate, productionType,
                        sourceProduct, sourceStockType, note));

        processMaterialDeduction(log, product, sourceProduct, productionType, sourceProductId,
                sourceStockType, producedQuantity, productionDate);

        return log; // product.increaseStock() 호출 없음
    }

    private Product resolveSourceProduct(Long sourceProductId) {
        return sourceProductId != null
                ? EntityFinder.findOrThrow(productRepository, sourceProductId, "원본 완제품")
                : null;
    }

    private void processMaterialDeduction(ProductionLog log, Product product, Product sourceProduct,
                                           ProductionType productionType, Long sourceProductId,
                                           SourceStockType sourceStockType,
                                           int producedQuantity, LocalDate productionDate) {
        if (productionType == ProductionType.CONVERSION) {
            // 원본 재고를 보류재고에서 뺄지, 당일 생산분(현재고)에서 뺄지 - 매번 선택된 값을 따른다
            if (sourceStockType == SourceStockType.RESERVED) {
                sourceProduct.decreaseReservedStock(producedQuantity);
            } else {
                sourceProduct.decreaseStock(producedQuantity);
            }

            List<ProductConversionRecipe> conversionItems =
                    conversionRecipeRepository.findBySourceAndTarget(sourceProductId, product.getId());
            Validate.notEmpty(conversionItems, "전환 레시피");

            for (ProductConversionRecipe item : conversionItems) {
                deductMaterialAndSnapshot(log, item.getMaterial(),
                        item.calculateRequiredAmount(producedQuantity), product, producedQuantity, productionDate);
            }
        } else {
            List<ProductRecipe> recipe = recipeRepository.findByProductId(product.getId());
            Validate.notEmpty(recipe, "레시피");

            for (ProductRecipe item : recipe) {
                BigDecimal requiredAmount = item.calculatePerUnitQuantity().multiply(BigDecimal.valueOf(producedQuantity));
                deductMaterialAndSnapshot(log, item.getMaterial(), requiredAmount, product, producedQuantity, productionDate);
            }
        }
    }

    private void deductMaterialAndSnapshot(ProductionLog log, Material material, BigDecimal requiredAmount,
                                            Product product, int producedQuantity, LocalDate productionDate) {
        material.decreaseStock(requiredAmount);

        materialTransactionRepository.save(MaterialStockTransaction.register(
                material, StockTransactionType.OUT, StockReasonCode.PRODUCTION_CONSUMPTION,
                requiredAmount, "생산: " + product.getName() + " " + producedQuantity + "개", productionDate));

        usageRepository.save(ProductionMaterialUsage.register(log, material, requiredAmount));
    }

    /**
     * 생산 취소. instantRecord면 완제품 재고 복원 단계를 건너뛴다.
     * CONVERSION이었으면 sourceStockType을 보고 원본 완제품의 보류재고인지 당일재고인지
     * 정확히 그 자리에 복원한다. 원재료는 스냅샷 기준 정확히 복원한다.
     */
    @Transactional
    public void cancel(Long productionLogId) {
        ProductionLog log = productionLogRepository.findByIdWithProducts(productionLogId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 생산 기록입니다. id=" + productionLogId));

        if (!log.isInstantRecord()) {
            closingService.assertNotClosed(DEFAULT_STORE_ID, log.getProductionDate());
        }

        log.cancel();

        Product product = log.getProduct();
        if (!log.isInstantRecord()) {
            product.decreaseStock(log.getProducedQuantity());
        }

        if (log.getProductionType() == ProductionType.CONVERSION) {
            if (log.getSourceStockType() == SourceStockType.RESERVED) {
                log.getSourceProduct().increaseReservedStock(log.getProducedQuantity());
            } else {
                log.getSourceProduct().increaseStock(log.getProducedQuantity());
            }
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
