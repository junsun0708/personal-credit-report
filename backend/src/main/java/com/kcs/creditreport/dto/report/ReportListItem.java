package com.kcs.creditreport.dto.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kcs.creditreport.domain.CreditReport;
import java.time.LocalDate;

/**
 * 리포트 목록 항목(요약) DTO (API 명세서 §3.5).
 *
 * <p>목록 응답에는 민감정보(주민번호 등)를 <b>절대 포함하지 않는다</b>. 상세 항목은
 * {@link ReportDetail} 참조. 엔티티 직접 직렬화를 금지하기 위해 본 DTO 로만 변환해 응답한다.
 *
 * @param id          리포트 ID
 * @param title       리포트 제목
 * @param institution 발급기관명
 * @param creditScore 신용점수
 * @param creditGrade 신용등급(1~10)
 * @param issuedDate  발급일({@code yyyy-MM-dd})
 */
public record ReportListItem(
        Long id,
        String title,
        String institution,
        Integer creditScore,
        Integer creditGrade,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate issuedDate
) {

    /** 엔티티 → 목록 항목 DTO 변환(민감정보 제외). */
    public static ReportListItem from(CreditReport report) {
        return new ReportListItem(
                report.getId(),
                report.getTitle(),
                report.getInstitution(),
                report.getCreditScore(),
                report.getCreditGrade(),
                report.getIssuedDate());
    }
}
