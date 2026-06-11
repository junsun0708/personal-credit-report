package com.kcs.creditreport.dto.report;

import com.kcs.creditreport.domain.CreditReport;
import com.kcs.creditreport.domain.ViewHistory;
import java.time.LocalDateTime;

/**
 * 조회 이력 항목 DTO (API 명세서 §3.7).
 *
 * <p>이력은 본인 것만 최신순으로 페이징되며, 각 항목은 연관된 리포트 요약(제목·기관·등급)과
 * 조회 시각을 포함한다. 엔티티 직접 직렬화를 금지하기 위해 본 DTO 로만 변환한다.
 *
 * @param id          이력 ID
 * @param reportId    조회한 리포트 ID(상세 페이지 링크용)
 * @param reportTitle 리포트 제목
 * @param institution 발급기관명
 * @param creditGrade 신용등급(1~10)
 * @param viewedAt    조회 시각(ISO-8601)
 */
public record HistoryItem(
        Long id,
        Long reportId,
        String reportTitle,
        String institution,
        Integer creditGrade,
        LocalDateTime viewedAt
) {

    /**
     * 엔티티 → 이력 항목 DTO 변환.
     *
     * <p>연관 {@link CreditReport} 의 지연 로딩 프로퍼티(title·institution·creditGrade)에
     * 접근하므로, 호출 서비스는 트랜잭션 경계 내에서(또는 fetch join 으로) 변환해야 한다.
     */
    public static HistoryItem from(ViewHistory history) {
        CreditReport report = history.getReport();
        return new HistoryItem(
                history.getId(),
                report.getId(),
                report.getTitle(),
                report.getInstitution(),
                report.getCreditGrade(),
                history.getViewedAt());
    }
}
