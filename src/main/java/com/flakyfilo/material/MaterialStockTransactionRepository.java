package com.flakyfilo.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaterialStockTransactionRepository extends JpaRepository<MaterialStockTransaction, Long> {

    // 기간(from~to)으로 걸러서 조회 - 이력이 계속 쌓이는 데이터라 처음부터 서버에서 기간 필터링
    List<MaterialStockTransaction> findByMaterialIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long materialId, LocalDate from, LocalDate to);
}