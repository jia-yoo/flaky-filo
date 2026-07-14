package com.flakyfilo.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    // 일일 생산/마감 화면에서 쓰는 목록 - 판매중(active=true)인 것만
    List<Product> findByStoreIdAndActiveTrue(Long storeId);
}
