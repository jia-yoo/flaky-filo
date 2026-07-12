package com.flakyfilo.common;

import com.flakyfilo.common.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;

/**
// * "id로 조회하고, 없으면 BusinessException" 패턴이 모든 Service마다 반복돼서 뽑아낸 공통 헬퍼.
 * JpaRepository<T, ID>를 쓰는 어떤 Repository/Entity 조합에도 재사용 가능하다.
 */
public class EntityFinder {

    private EntityFinder() {
    }

    public static <T, ID> T findOrThrow(JpaRepository<T, ID> repository, ID id, String entityName) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 " + entityName + "입니다. id=" + id));
    }
}