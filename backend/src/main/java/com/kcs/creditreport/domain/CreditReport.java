package com.kcs.creditreport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 신용평가 리포트 엔티티. (설계서 §3.2)
 *
 * <p>모든 도메인 쿼리는 권한 격리상 {@code user_id} 로 스코프된다. 이를 위해
 * 검색/필터/정렬 컬럼을 선두 {@code user_id} 와 결합한 복합 인덱스를 선언한다.
 * (설계서 §4 인덱스 설계)
 *
 * <p>{@code ssn}(주민등록번호)은 과제 기준 평문 저장하되, 응답 DTO 변환 시점에
 * 마스킹한다(설계서 §6). 엔티티 원본은 응답 직렬화 대상에서 제외해야 한다.
 */
@Entity
@Table(
        name = "credit_report",
        indexes = {
                @Index(name = "idx_report_user", columnList = "user_id"),
                @Index(name = "idx_report_user_issued", columnList = "user_id, issued_date"),
                @Index(name = "idx_report_user_score", columnList = "user_id, credit_score"),
                @Index(name = "idx_report_user_grade", columnList = "user_id, credit_grade")
        }
)
// 점수·등급·연체건수 범위 무결성 (설계서 §5.2 CHECK 제약)
@Check(constraints = "credit_grade BETWEEN 1 AND 10 "
        + "AND credit_score BETWEEN 0 AND 1000 "
        + "AND delinquency_count >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditReport {

    /** 리포트 식별자 (PK, AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소유 회원 (권한 격리 기준).
     * 양방향 불필요하여 단방향 ManyToOne, 지연 로딩.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 리포트 제목 (검색 대상, LIKE 부분 일치). */
    @Column(nullable = false, length = 200)
    private String title;

    /** 발급기관명 (검색 대상, LIKE 부분 일치). */
    @Column(nullable = false, length = 100)
    private String institution;

    /** 신용점수 (NICE식 0~1000). */
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    /** 신용등급 (1=최우량 ~ 10=최저). */
    @Column(name = "credit_grade", nullable = false)
    private Integer creditGrade;

    /** 주민등록번호 (YYMMDD-XXXXXXX, 14자). 평문 저장·응답 시 마스킹. */
    @Column(nullable = false, length = 14)
    private String ssn;

    /** 발급일자 (필터·정렬 대상). */
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    /** 리포트 요약/총평. */
    @Column(name = "report_summary", length = 1000)
    private String reportSummary;

    /** 연체 건수. */
    @Column(name = "delinquency_count", nullable = false)
    private Integer delinquencyCount;

    /** 총 부채액(원). */
    @Column(name = "total_debt", nullable = false)
    private Long totalDebt;

    /** 보유 신용카드 수. */
    @Column(name = "credit_card_count", nullable = false)
    private Integer creditCardCount;

    /** 대출 건수. */
    @Column(name = "loan_count", nullable = false)
    private Integer loanCount;

    /** 생성 일시. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private CreditReport(User user, String title, String institution, Integer creditScore,
                         Integer creditGrade, String ssn, LocalDate issuedDate, String reportSummary,
                         Integer delinquencyCount, Long totalDebt, Integer creditCardCount,
                         Integer loanCount) {
        this.user = user;
        this.title = title;
        this.institution = institution;
        this.creditScore = creditScore;
        this.creditGrade = creditGrade;
        this.ssn = ssn;
        this.issuedDate = issuedDate;
        this.reportSummary = reportSummary;
        // 카운트/금액 항목은 null 방어 — 미지정 시 0
        this.delinquencyCount = delinquencyCount != null ? delinquencyCount : 0;
        this.totalDebt = totalDebt != null ? totalDebt : 0L;
        this.creditCardCount = creditCardCount != null ? creditCardCount : 0;
        this.loanCount = loanCount != null ? loanCount : 0;
    }
}
