package com.flakyfilo.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialStockTransactionRepository extends JpaRepository<MaterialStockTransaction, Long> {

    List<MaterialStockTransaction> findByMaterialIdOrderByCreatedAtDesc(Long materialId);
}
