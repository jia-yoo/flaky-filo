package com.flakyfilo.production;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.enums.StockReasonCode;
import com.flakyfilo.common.enums.StockTransactionType;
import com.flakyfilo.common.exception.BusinessException;
import com.flakyfilo.material.Material;
import com.flakyfilo.material.MaterialStockTransaction;
import com.flakyfilo.material.MaterialStockTransactionRepository;
import com.flakyfilo.product.Product;
import com.flakyfilo.product.ProductRecipe;
import com.flakyfilo.product.ProductRecipeRepository;
import com.flakyfilo.product.ProductRepository;
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
    private final ProductionLogRepository productionLogRepository;
    private final ProductionMaterialUsageRepository usageRepository;
    private final MaterialStockTransactionRepository materialTransactionRepository;

    /**
     * 생산 등록 - "오늘 이 완제품을 N개 구웠다"를 기록하고, 레시피(BOM) 기준으로
     * 원재료를 자동 차감한다. 원재료가 부족한 항목이 하나라도 있으면 전체가 롤백된다.
     */
    @Transactional
    public ProductionLog register(Long productId, int producedQuantity, LocalDate productionDate, String note) {
        Product product = EntityFinder.findOrThrow(productRepository, productId, "완제품");
        List<ProductRecipe> recipe = recipeRepository.findByProductId(productId);

        if (recipe.isEmpty()) {
            throw new BusinessException("레시피가 등록되지 않아 생산할 수 없습니다. 먼저 레시피를 등록하세요.");
        }

        ProductionLog log = productionLogRepository.save(
                ProductionLog.register(product, producedQuantity, productionDate, note));

        for (ProductRecipe recipeItem : recipe) {
            Material material = recipeItem.getMaterial();
            BigDecimal requiredAmount = recipeItem.calculatePerUnitQuantity()
                    .multiply(BigDecimal.valueOf(producedQuantity));

            material.decreaseStock(requiredAmount); // 부족하면 여기서 예외 -> 트랜잭션 전체 롤백

            materialTransactionRepository.save(MaterialStockTransaction.register(
                    material, StockTransactionType.OUT, StockReasonCode.PRODUCTION_CONSUMPTION,
                    requiredAmount, "생산: " + product.getName() + " " + producedQuantity + "개", productionDate));

            // 나중에 취소할 때 정확히 복원할 수 있도록, "이 생산 건에 실제로 쓴 양"을 스냅샷으로 저장
            usageRepository.save(ProductionMaterialUsage.register(log, material, requiredAmount));
        }

        product.increaseStock(producedQuantity);
        return log;
    }

    /**
     * 생산 취소. 오입력했을 때 이걸로 되돌리고 다시 register()로 정확하게 등록한다.
     * 레시피가 그 사이 바뀌었어도, ProductionMaterialUsage 스냅샷 기준으로 정확히 복원한다.
     */
    @Transactional
    public void cancel(Long productionLogId) {
        ProductionLog log = EntityFinder.findOrThrow(productionLogRepository, productionLogId, "생산 기록");
        log.cancel(); // 이미 취소된 경우 여기서 예외

        Product product = log.getProduct();
        product.decreaseStock(log.getProducedQuantity()); // 이미 판매돼서 재고가 부족하면 여기서 막힘

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
