package com.flakyfilo.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // items와 그 안의 product까지 fetch join - 목록/상세 조회 시 LazyInitializationException, N+1 방지
    @Query("select distinct o from Order o left join fetch o.items i left join fetch i.product where o.id = :id")
    Optional<Order> findByIdWithItems(Long id);

    @Query("select distinct o from Order o left join fetch o.items i left join fetch i.product " +
           "where o.orderDate between :from and :to order by o.orderDate desc, o.id desc")
    List<Order> findByOrderDateBetween(LocalDate from, LocalDate to);
}
