package com.flakyfilo.product;

import com.flakyfilo.common.EntityFinder;
import com.flakyfilo.common.Validate;
import com.flakyfilo.material.Material;
import com.flakyfilo.material.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public Product register(Long storeId, String name, String category, int price,
                             Integer yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio) {
        return productRepository.save(Product.register(
                storeId, name, category, price, yieldCount, overheadRate, targetCostRatio));
    }

    public Product getById(Long productId) {
        return findOrThrow(productId);
    }

    public List<Product> getAll(Long storeId) {
        return productRepository.findByStoreId(storeId);
    }

    @Transactional
    public void updateInfo(Long productId, String name, String category, int price,
                            int yieldCount, BigDecimal overheadRate, BigDecimal targetCostRatio) {
        Product product = findOrThrow(productId);
        product.updateInfo(name, category, price, yieldCount, overheadRate, targetCostRatio);
    }

    @Transactional
    public void delete(Long productId) {
        Product product = findOrThrow(productId);
        recipeRepository.deleteByProductId(productId);
        productRepository.delete(product);
    }

    /**
     * 레시피(BOM)를 통째로 교체 등록한다. 배치 전체 기준 수량을 그대로 여러 번 고쳐볼 수 있다.
     * 같은 원재료가 레시피에 여러 줄로 중복 등록되는 것도 허용한다
     * (케이크 시트용/크림용처럼 실제 레시피 그대로 나눠 적는 게 더 편할 수 있어서).
     */
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

    /**
     * 원가 계산. 시트에서 쓰던 공식 그대로:
     * 원가(rawCost) = 레시피 항목별 개당 원가 합계
     * 원가율 = 원가 ÷ 판매가
     * 기타경비 = 원가 × 기타경비율
     * 총원가 = 원가 + 기타경비
     * 최소판매가 = 총원가 ÷ 목표원가율
     */
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
                : null; // 판매가 미정이면 원가율 계산 불가 (0으로 나눌 수 없음)

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
