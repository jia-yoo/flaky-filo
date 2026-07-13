package com.flakyfilo.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {

    @Query("select p from ProductionLog p join fetch p.product where p.product.id = :productId order by p.productionDate desc")
    List<ProductionLog> findByProductId(Long productId);
}
