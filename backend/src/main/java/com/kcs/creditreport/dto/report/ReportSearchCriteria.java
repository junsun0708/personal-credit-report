package com.kcs.creditreport.dto.report;

import java.time.LocalDate;

/**
 * 리포트 목록 검색/필터/정렬 조건 (API 명세서 §3.5, §5).
 *
 * <p>컨트롤러가 쿼리 파라미터를 검증·파싱한 결과를 담아 서비스로 전달하는 불변 값 객체다.
 * 각 필드는 미지정(null)일 수 있으며, 미지정 항목은 해당 조건을 적용하지 않는다.
 *
 * <p>{@code page} 는 명세상 1-based 입력값이며, 서비스에서 Spring {@code Pageable}(0-based)
 * 로 변환한다.
 *
 * @param q     검색어(title 또는 institution 부분 일치). null/공백이면 미적용.
 * @param grade 신용등급 필터(1~10). null 이면 미적용.
 * @param from  발급일 시작(포함). null 이면 미적용.
 * @param to    발급일 종료(포함). null 이면 미적용.
 * @param sort  정렬 기준({@code issuedDate} | {@code creditScore}).
 * @param order 정렬 방향({@code asc} | {@code desc}).
 * @param page  페이지 번호(1-based).
 * @param size  페이지당 항목 수.
 */
public record ReportSearchCriteria(
        String q,
        Integer grade,
        LocalDate from,
        LocalDate to,
        SortKey sort,
        SortOrder order,
        int page,
        int size
) {

    /** 허용 정렬 기준 — 미허용 입력은 컨트롤러에서 400 처리하기 위해 enum 으로 제한한다. */
    public enum SortKey {
        ISSUED_DATE("issuedDate"),
        CREDIT_SCORE("creditScore");

        /** 엔티티 프로퍼티명(정렬 컬럼 매핑용). */
        private final String property;

        SortKey(String property) {
            this.property = property;
        }

        public String property() {
            return property;
        }
    }

    /** 허용 정렬 방향. */
    public enum SortOrder {
        ASC,
        DESC
    }
}
