package com.kcs.creditreport.dto.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kcs.creditreport.domain.CreditReport;
import com.kcs.creditreport.util.SsnMasker;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 리포트 상세 응답 DTO (API 명세서 §3.6).
 *
 * <p>주민등록번호는 {@code maskedSsn} (마스킹된 값)만 포함하며, 평문 {@code ssn} 은 절대
 * 직렬화하지 않는다(설계서 §6.2 — 서버 응답 단계 마스킹). 명세상 명의자명({@code holderName})은
 * 별도 컬럼이 없어 발급기관명을 노출하지 않는 선에서 제공하지 않는다 — 아래 빌더 주석 참조.
 *
 * @param id          리포트 ID
 * @param title       리포트 제목
 * @param institution 발급기관명
 * @param creditScore 신용점수(게이지 시각화 대상)
 * @param creditGrade 신용등급(1~10)
 * @param issuedDate  발급일({@code yyyy-MM-dd})
 * @param maskedSsn   마스킹된 주민등록번호(예: {@code 900101-1******})
 * @param reportSummary 리포트 요약/총평
 * @param delinquencyCount 연체 건수(상세 시각화)
 * @param totalDebt   총 부채액(원)
 * @param creditCardCount 보유 신용카드 수
 * @param loanCount   대출 건수
 * @param createdAt   리포트 생성 시각(ISO-8601)
 */
public record ReportDetail(
        Long id,
        String title,
        String institution,
        Integer creditScore,
        Integer creditGrade,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate issuedDate,
        String maskedSsn,
        String reportSummary,
        Integer delinquencyCount,
        Long totalDebt,
        Integer creditCardCount,
        Integer loanCount,
        LocalDateTime createdAt
) {

    /**
     * 엔티티 → 상세 응답 DTO 변환.
     *
     * <p>주민번호는 {@link SsnMasker#mask(String)} 로 마스킹한 값만 담는다(평문 미포함).
     * 엔티티 원본 {@code ssn} 은 본 DTO 어디에도 포함하지 않으므로 평문이 직렬화되지 않는다.
     */
    public static ReportDetail from(CreditReport report) {
        return new ReportDetail(
                report.getId(),
                report.getTitle(),
                report.getInstitution(),
                report.getCreditScore(),
                report.getCreditGrade(),
                report.getIssuedDate(),
                SsnMasker.mask(report.getSsn()),
                report.getReportSummary(),
                report.getDelinquencyCount(),
                report.getTotalDebt(),
                report.getCreditCardCount(),
                report.getLoanCount(),
                report.getCreatedAt());
    }
}
