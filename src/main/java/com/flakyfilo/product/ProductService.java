package com.flakyfilo.product;

import com.flakyfilo.closing.ClosingActionLog;
import com.flakyfilo.closing.ClosingActionLogRepository;
import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.Validate;
import com.flakyfilo.common.enums.ClosingActionType;
import com.flakyfilo.common.enums.OrderChannel;
import com.flakyfilo.material.Material;
import com.flakyfilo.material.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final ProductConversionRecipeRepository conversionRecipeRepository;
    private final ProductChannelPriceRepository channelPriceRepository;
    private final ClosingActionLogRepository closingActionLogRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public Product register(Long storeId, String name, String category, int price,
                            Integer yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio,
                            Boolean active, Boolean autoDisposeIfUnsold) {
        return productRepository.save(Product.register(
                storeId, name, category, price, yieldCount, overheadRate, targetCostRatio,
                active, autoDisposeIfUnsold));
    }

    public Product getById(Long productId) {
        return findOrThrow(productId);
    }

    public List<Product> getAll(Long storeId) {
        return productRepository.findByStoreId(storeId);
    }

    /** 일일 생산/마감 화면 전용 - 판매중인 것만 */
    public List<Product> getActiveProducts(Long storeId) {
        return productRepository.findByStoreIdAndActiveTrue(storeId);
    }

    @Transactional
    public void updateInfo(Long productId, String name, String category, int price,
                           int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio,
                           boolean active, boolean autoDisposeIfUnsold) {
        Product product = findOrThrow(productId);
        product.updateInfo(name, category, price, yieldCount, overheadRate, targetCostRatio,
                active, autoDisposeIfUnsold);
    }

    @Transactional
    public void delete(Long productId) {
        Product product = findOrThrow(productId);
        recipeRepository.deleteByProductId(productId);
        productRepository.delete(product);
    }

    @Transactional
    public void setRecipe(Long productId, List<RecipeItem> items) {
        Product product = findOrThrow(productId);
        recipeRepository.deleteByProductId(productId);

        for (RecipeItem item : items) {
            Validate.strictlyPositive(item.batchQuantity(), "필요량");
            Material material = EntityFinder.findOrThrow(materialRepository, item.materialId(), "원재료");
            recipeRepository.save(ProductRecipe.register(product, material, item.batchQuantity()));
        }
    }

    public List<ProductRecipe> getRecipe(Long productId) {
        return recipeRepository.findByProductId(productId);
    }

    /** 전환 레시피(원본→대상 조합별 차이분 재료) 통째로 교체 등록 - setRecipe와 같은 방식(덮어쓰기). */
    @Transactional
    public void setConversionRecipe(Long sourceProductId, Long targetProductId, List<RecipeItem> items) {
        Product source = findOrThrow(sourceProductId);
        Product target = findOrThrow(targetProductId);
        conversionRecipeRepository.deleteBySourceProductIdAndTargetProductId(sourceProductId, targetProductId);

        // items가 null이면 그냥 빈 리스트로 취급 - "삭제만 하고 새로 안 채운다"는 뜻이 되어 정상 동작
        List<RecipeItem> safeItems = items != null ? items : List.of();
        for (RecipeItem item : safeItems) {
            Validate.strictlyPositive(item.batchQuantity(), "필요량");
            Material material = EntityFinder.findOrThrow(materialRepository, item.materialId(), "원재료");
            conversionRecipeRepository.save(
                    ProductConversionRecipe.register(source, target, material, item.batchQuantity()));
        }
    }

    public List<ProductConversionRecipe> getConversionRecipe(Long sourceProductId, Long targetProductId) {
        return conversionRecipeRepository.findBySourceAndTarget(sourceProductId, targetProductId);
    }

    /** 이 완제품에 등록된 전환 레시피를 원본 무관하게 전체 조회 (모달 열 때 자동으로 보여주기 위함). */
    public List<ProductConversionRecipe> getConversionRecipesByTarget(Long targetProductId) {
        return conversionRecipeRepository.findByTargetProductId(targetProductId);
    }

    /** 마감 보류: 당일 안 팔린 만큼 판매 재고에서 보류 재고로 옮김. 마감 취소 시 되돌릴 수 있게 기록도 남긴다. */
    @Transactional
    public void reserveStock(Long productId, int quantity, LocalDate closingDate) {
        Product product = findOrThrow(productId);
        product.reserveStock(quantity);
        closingActionLogRepository.save(ClosingActionLog.register(product, closingDate, ClosingActionType.RESERVE, quantity));
    }

    /** 결국 못 쓰게 된 보류 재고 폐기 (완제품 관리 화면에서 예외적으로 쓰는 액션 - 마감 취소 대상 아님). */
    @Transactional
    public void wasteReservedStock(Long productId, int quantity) {
        Product product = findOrThrow(productId);
        product.wasteReservedStock(quantity);
    }

    /** 마감 즉시 폐기 - 보류를 거치지 않고 당일 남은 재고를 바로 버림. 마감 취소 시 되돌릴 수 있게 기록도 남긴다. */
    @Transactional
    public void disposeStock(Long productId, int quantity, LocalDate closingDate) {
        Product product = findOrThrow(productId);
        product.disposeStock(quantity);
        closingActionLogRepository.save(ClosingActionLog.register(product, closingDate, ClosingActionType.WASTE, quantity));
    }

    /** 채널별 가격 등록/수정 (있으면 갱신, 없으면 새로 생성 - "upsert"). */
    @Transactional
    public void setChannelPrice(Long productId, OrderChannel channel, int price) {
        Product product = findOrThrow(productId);
        ProductChannelPrice existing = channelPriceRepository.findByProductIdAndChannel(productId, channel)
                .orElse(null);

        if (existing != null) {
            existing.updatePrice(price);
        } else {
            channelPriceRepository.save(ProductChannelPrice.register(product, channel, price));
        }
    }

    /** 채널별 가격 삭제 - 삭제하면 그 채널은 다시 Product.price(매장 기본가)를 따르게 됨. */
    @Transactional
    public void removeChannelPrice(Long productId, OrderChannel channel) {
        channelPriceRepository.deleteByProductIdAndChannel(productId, channel);
    }

    public List<ProductChannelPrice> getChannelPrices(Long productId) {
        return channelPriceRepository.findByProductId(productId);
    }

    public CostResult calculateCost(Long productId) {
        Product product = findOrThrow(productId);
        List<ProductRecipe> recipe = recipeRepository.findByProductId(productId);

        BigDecimal rawCost = recipe.stream()
                .map(ProductRecipe::calculateCostContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overhead = rawCost.multiply(product.getOverheadRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = rawCost.add(overhead);

        BigDecimal costRatio = product.getPrice() > 0
                ? rawCost.divide(BigDecimal.valueOf(product.getPrice()), 4, RoundingMode.HALF_UP)
                : null;

        BigDecimal minPrice = totalCost.divide(product.getTargetCostRatio(), 2, RoundingMode.HALF_UP);
        BigDecimal margin = BigDecimal.valueOf(product.getPrice()).subtract(totalCost);

        return new CostResult(rawCost, overhead, totalCost, costRatio, minPrice, margin, product.getPrice());
    }

    private Product findOrThrow(Long productId) {
        return EntityFinder.findOrThrow(productRepository, productId, "완제품");
    }

    public record RecipeItem(Long materialId, BigDecimal batchQuantity) {
    }

    public record CostResult(BigDecimal rawCost, BigDecimal overhead, BigDecimal totalCost,
                             BigDecimal costRatio, BigDecimal minPrice, BigDecimal margin, int price) {
    }
}