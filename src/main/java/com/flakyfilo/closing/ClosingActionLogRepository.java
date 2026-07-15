package com.flakyfilo.closing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ClosingActionLogRepository extends JpaRepository<ClosingActionLog, Long> {

    @Query("select c from ClosingActionLog c join fetch c.product where c.closingDate = :date and c.reversed = false")
    List<ClosingActionLog> findByClosingDateAndNotReversed(LocalDate date);
}
