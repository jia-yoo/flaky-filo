package com.flakyfilo.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    // 주문 화면 등에서 쓰는 전체 판매중 목록 (즉석메뉴 포함)
    List<Product> findByStoreIdAndActiveTrue(Long storeId);

    // 일일 생산/마감 화면 전용 - 즉석주문생산 메뉴는 이 사이클 대상이 아니라서 제외
    List<Product> findByStoreIdAndActiveTrueAndInstantProductionFalse(Long storeId);

    // 즉석메뉴 빠른 기록 패널 전용 - 즉석주문생산 메뉴만
    List<Product> findByStoreIdAndActiveTrueAndInstantProductionTrue(Long storeId);
}
