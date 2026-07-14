package com.flakyfilo.product;

import com.flakyfilo.common.enums.OrderChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductChannelPriceRepository extends JpaRepository<ProductChannelPrice, Long> {

    Optional<ProductChannelPrice> findByProductIdAndChannel(Long productId, OrderChannel channel);

    List<ProductChannelPrice> findByProductId(Long productId);

    void deleteByProductIdAndChannel(Long productId, OrderChannel channel);
}