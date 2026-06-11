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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 조회 이력 엔티티. (설계서 §3.3)
 *
 * <p>users 와 credit_report 사이의 조회 행위(M:N)를 "시점(viewed_at)"을 가진
 * 연결 엔티티로 분해한 형태. 동일 {@code (user, report)} 조합이라도 상세 진입 시마다
 * 새 행을 INSERT 한다 (UNIQUE 제약 없음).
 */
@Entity
@Table(
        name = "view_history",
        indexes = {
                @Index(name = "idx_history_user_viewed", columnList = "user_id, viewed_at"),
                @Index(name = "idx_history_report", columnList = "report_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewHistory {

    /** 이력 식별자 (PK, AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 조회한 회원. 단방향 ManyToOne, 지연 로딩. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 조회된 리포트. 단방향 ManyToOne, 지연 로딩. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private CreditReport report;

    /** 조회 시점. 영속화 시점에 자동 기록. */
    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @Builder
    private ViewHistory(User user, CreditReport report) {
        this.user = user;
        this.report = report;
    }

    /**
     * 조회 이력 기록용 정적 팩토리.
     *
     * @param user   조회한 회원
     * @param report 조회된 리포트
     */
    public static ViewHistory of(User user, CreditReport report) {
        return ViewHistory.builder()
                .user(user)
                .report(report)
                .build();
    }
}
