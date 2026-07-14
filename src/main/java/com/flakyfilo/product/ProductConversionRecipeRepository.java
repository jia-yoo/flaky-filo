package com.flakyfilo.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductConversionRecipeRepository extends JpaRepository<ProductConversionRecipe, Long> {

    @Query("select r from ProductConversionRecipe r join fetch r.material join fetch r.sourceProduct " +
            "where r.sourceProduct.id = :sourceProductId and r.targetProduct.id = :targetProductId")
    List<ProductConversionRecipe> findBySourceAndTarget(Long sourceProductId, Long targetProductId);

    // 모달 열 때 "이 완제품에 이미 등록된 전환 레시피가 있는지" 원본 상관없이 한 번에 확인하는 용도
    @Query("select r from ProductConversionRecipe r join fetch r.material join fetch r.sourceProduct " +
            "where r.targetProduct.id = :targetProductId")
    List<ProductConversionRecipe> findByTargetProductId(Long targetProductId);

    void deleteBySourceProductIdAndTargetProductId(Long sourceProductId, Long targetProductId);
}