package com.kcs.creditreport.repository.specification;

import com.kcs.creditreport.domain.CreditReport;
import com.kcs.creditreport.dto.report.ReportSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 리포트 목록 동적 쿼리 Specification 빌더 (API 명세서 §5.5 적용 순서).
 *
 * <p>모든 조건은 {@code WHERE} 절에서 <b>AND</b> 로 결합되며, 권한 스코프({@code user_id})는
 * <b>항상</b> 선행 결합된다(설계서 §5.3 — 클라이언트 필터링 금지, 서버 동적 쿼리).
 *
 * <pre>
 * [권한 스코프] WHERE user_id = :currentUserId
 *   → [검색]   AND (LOWER(title) LIKE %q% OR LOWER(institution) LIKE %q%)
 *   → [필터]   AND credit_grade = :grade
 *              AND issued_date &gt;= :from AND issued_date &lt;= :to
 * </pre>
 *
 * <p>정렬/페이징은 {@code Pageable} 로 별도 처리하므로 본 클래스는 {@code WHERE} 절만 구성한다.
 */
public final class CreditReportSpecifications {

    private CreditReportSpecifications() {
        // 유틸 클래스 — 인스턴스화 금지
    }

    /**
     * 권한 스코프 + 검색/필터 조건을 결합한 단일 Specification 을 생성한다.
     *
     * <p>{@code currentUserId} 는 권한 격리를 위해 <b>필수</b>이며 항상 첫 조건으로 결합된다.
     * 모든 조건을 하나의 {@code toPredicate} 안에서 누적해, JPA Criteria 가 단일
     * {@code WHERE ... AND ...} 로 평탄화하도록 한다(중첩 Specification 의 불필요한 괄호 방지).
     *
     * @param currentUserId 현재 인증 사용자 ID(권한 스코프 — 필수)
     * @param criteria      검색/필터 조건(미지정 항목은 건너뜀)
     */
    public static Specification<CreditReport> forUser(Long currentUserId, ReportSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // [권한 스코프] 항상 user_id 결합 — 타인 데이터 누수 차단(설계서 §5.3)
            predicates.add(cb.equal(root.get("user").get("id"), currentUserId));

            // [검색] q: title 또는 institution 대소문자 무시 부분 일치(LIKE %q%)
            String q = criteria.q();
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), like);
                Predicate institutionLike = cb.like(cb.lower(root.get("institution")), like);
                predicates.add(cb.or(titleLike, institutionLike));
            }

            // [필터] grade: 정확 일치(범위 검증은 컨트롤러에서 선행)
            Integer grade = criteria.grade();
            if (grade != null) {
                predicates.add(cb.equal(root.get("creditGrade"), grade));
            }

            // [필터] from: issued_date >= from (포함)
            LocalDate from = criteria.from();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issuedDate"), from));
            }

            // [필터] to: issued_date <= to (포함)
            LocalDate to = criteria.to();
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issuedDate"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
