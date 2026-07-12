package com.flakyfilo.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    // fetch join(join fetch m.supplier)으로 원재료 목록 조회 시 구매처 정보까지 한 번의 쿼리로 같이 가져온다.
    // 이걸 안 하면 300건 조회할 때 구매처 이름 꺼내려고 300번의 추가 쿼리가 나가는 N+1 문제가 생긴다.
    @Query("select m from Material m left join fetch m.supplier where m.storeId = :storeId")
    List<Material> findByStoreId(Long storeId);

    @Query("select m from Material m left join fetch m.supplier where m.currentStock <= m.minStockThreshold and m.storeId = :storeId")
    List<Material> findLowStock(Long storeId);

    @Query("select m from Material m left join fetch m.supplier where m.id = :id")
    Optional<Material> findByIdWithSupplier(Long id);
}