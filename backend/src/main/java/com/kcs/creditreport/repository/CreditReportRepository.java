package com.kcs.creditreport.repository;

import com.kcs.creditreport.domain.CreditReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 신용평가 리포트 Repository.
 *
 * <p>{@link JpaSpecificationExecutor} 를 상속하여 Phase 3 의 동적 검색/필터/정렬
 * (Specification 기반 + Pageable)을 지원한다.
 *
 * <p>권한 격리 원칙(설계서 §5.3): 상세 조회는 {@code findById} 단독 사용을 금지하고
 * 반드시 {@code userId} 를 결합한 {@link #findByIdAndUserId} 를 사용한다.
 */
public interface CreditReportRepository
        extends JpaRepository<CreditReport, Long>, JpaSpecificationExecutor<CreditReport> {

    /**
     * 본인 소유 리포트 단건 조회 (권한 격리).
     * 타인 리포트 id 직접 요청 시 empty 반환 → 호출부에서 404/403 처리.
     */
    Optional<CreditReport> findByIdAndUserId(Long id, Long userId);
}
