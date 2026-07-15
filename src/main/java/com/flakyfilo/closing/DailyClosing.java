package com.flakyfilo.closing;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * "이 날짜는 마감 처리가 끝났다"는 표시. 이게 있으면 그 날짜로는
 * 생산 등록/취소(=수정)가 더 이상 안 되게 막는다 (마감 후 데이터가 계속 바뀌는 걸 방지).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_closing", uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "closing_date"}))
public class DailyClosing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "closing_date", nullable = false)
    private LocalDate closingDate;

    @Builder(access = AccessLevel.PRIVATE)
    private DailyClosing(Long storeId, LocalDate closingDate) {
        this.storeId = storeId;
        this.closingDate = closingDate;
    }

    public static DailyClosing register(Long storeId, LocalDate closingDate) {
        if (closingDate == null) {
            throw new BusinessException("마감 날짜는 필수입니다.");
        }
        return DailyClosing.builder().storeId(storeId).closingDate(closingDate).build();
    }
}
