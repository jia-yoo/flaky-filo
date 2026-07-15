package com.flakyfilo.closing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyClosingRepository extends JpaRepository<DailyClosing, Long> {

    boolean existsByStoreIdAndClosingDate(Long storeId, LocalDate closingDate);

    Optional<DailyClosing> findByStoreIdAndClosingDate(Long storeId, LocalDate closingDate);

    void deleteByStoreIdAndClosingDate(Long storeId, LocalDate closingDate);
}
