package com.flakyfilo.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {

    // material과 product 둘 다 fetch join - RecipeItemResponse.from()에서
    // material.getUnitCost()와 product.getYieldCount()를 둘 다 쓰기 때문에 둘 다 필요
    @Query("select r from ProductRecipe r join fetch r.material join fetch r.product where r.product.id = :productId")
    List<ProductRecipe> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    // 이 원재료를 사용 중인 레시피가 있는지 (Material 삭제 전 참조 확인용)
    boolean existsByMaterialId(Long materialId);
}