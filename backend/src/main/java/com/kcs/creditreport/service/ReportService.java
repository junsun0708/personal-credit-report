package com.kcs.creditreport.service;

import com.kcs.creditreport.domain.CreditReport;
import com.kcs.creditreport.domain.ViewHistory;
import com.kcs.creditreport.dto.common.PageResponse;
import com.kcs.creditreport.dto.report.ReportDetail;
import com.kcs.creditreport.dto.report.ReportListItem;
import com.kcs.creditreport.dto.report.ReportSearchCriteria;
import com.kcs.creditreport.exception.BusinessException;
import com.kcs.creditreport.exception.ErrorCode;
import com.kcs.creditreport.repository.CreditReportRepository;
import com.kcs.creditreport.repository.ViewHistoryRepository;
import com.kcs.creditreport.repository.specification.CreditReportSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신용평가 리포트 도메인 비즈니스 로직 (목록·상세).
 *
 * <p>핵심 원칙(설계서 §5.3): 모든 도메인 쿼리에 현재 사용자({@code currentUserId}) 스코프를
 * <b>강제</b>한다. 목록은 {@code JpaSpecificationExecutor} + {@link CreditReportSpecifications}
 * 동적 쿼리로, 상세는 {@code findByIdAndUserId} 로 권한을 격리한다(타인 ID 요청 → 404).
 *
 * <p>엔티티를 직접 직렬화하지 않고 반드시 응답 DTO 로 변환해 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CreditReportRepository creditReportRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    /**
     * 본인 소유 리포트 목록 조회 (페이징·검색·필터·정렬).
     *
     * <p>권한 스코프 + 검색/필터를 Specification 으로, 정렬/페이징을 {@code Pageable} 로 구성해
     * DB 측에서 처리한다(클라이언트 필터링 금지). 응답은 민감정보를 제외한 {@link ReportListItem}.
     *
     * @param currentUserId 현재 인증 사용자 ID(권한 스코프)
     * @param criteria      검증·파싱이 끝난 검색/필터/정렬/페이지 조건
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportListItem> getReports(Long currentUserId, ReportSearchCriteria criteria) {
        Specification<CreditReport> spec =
                CreditReportSpecifications.forUser(currentUserId, criteria);
        Pageable pageable = toPageable(criteria);

        Page<CreditReport> page = creditReportRepository.findAll(spec, pageable);
        return PageResponse.of(page, ReportListItem::from);
    }

    /**
     * 본인 소유 리포트 상세 조회 + 조회 이력 자동 INSERT.
     *
     * <p>권한 격리: {@code findByIdAndUserId} 로 본인 것만 조회하며, 없으면(타인 ID 포함)
     * {@code NOT_FOUND}(404)를 던진다(존재 여부 자체를 숨김 — 명세 §3.6 권한 격리).
     *
     * <p>부수효과: 조회 성공 시 같은 트랜잭션에서 {@link ViewHistory} 를 1건 INSERT 한다
     * (중복 조회도 매번 별도 행 기록 — 설계서 §3.3). 이력의 소유자는 리포트 소유자와 동일하므로
     * (권한 격리로 보장됨) 추가 User 조회 없이 {@code report.getUser()} 를 사용한다.
     *
     * @param currentUserId 현재 인증 사용자 ID(권한 스코프)
     * @param reportId      조회할 리포트 ID
     * @return 마스킹된 민감정보를 포함한 상세 응답 DTO
     */
    @Transactional
    public ReportDetail getReportDetail(Long currentUserId, Long reportId) {
        CreditReport report = creditReportRepository.findByIdAndUserId(reportId, currentUserId)
                .orElseThrow(() -> {
                    // 타인 소유/미존재 모두 동일하게 404 — 존재 여부 노출 차단(명세 §1.6, §3.6)
                    log.warn("리포트 상세 조회 실패(미존재 또는 타인 소유): reportId={}, userId={}",
                            reportId, currentUserId);
                    return new BusinessException(ErrorCode.NOT_FOUND);
                });

        // 조회 이력 기록 — report.getUser() 는 currentUserId 본인(권한 격리로 보장)
        viewHistoryRepository.save(ViewHistory.of(report.getUser(), report));

        return ReportDetail.from(report);
    }

    /**
     * 1-based page + 정렬 조건을 Spring {@code Pageable}(0-based)로 변환한다.
     *
     * <p>정렬 키/방향은 {@code ReportSearchCriteria} 의 enum 으로 이미 안전하게 제한되어 있으므로
     * 여기서는 추가 검증 없이 매핑만 수행한다(미허용 값 차단은 컨트롤러 책임).
     */
    private Pageable toPageable(ReportSearchCriteria criteria) {
        Sort.Direction direction = (criteria.order() == ReportSearchCriteria.SortOrder.ASC)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, criteria.sort().property());

        // page 는 1-based 입력 → 0-based 로 변환
        int zeroBasedPage = criteria.page() - 1;
        return PageRequest.of(zeroBasedPage, criteria.size(), sort);
    }
}
