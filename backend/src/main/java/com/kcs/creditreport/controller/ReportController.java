package com.kcs.creditreport.controller;

import com.kcs.creditreport.dto.common.PageResponse;
import com.kcs.creditreport.dto.report.ReportDetail;
import com.kcs.creditreport.dto.report.ReportListItem;
import com.kcs.creditreport.dto.report.ReportSearchCriteria;
import com.kcs.creditreport.dto.report.ReportSearchCriteria.SortKey;
import com.kcs.creditreport.dto.report.ReportSearchCriteria.SortOrder;
import com.kcs.creditreport.exception.BusinessException;
import com.kcs.creditreport.exception.ErrorCode;
import com.kcs.creditreport.security.AuthPrincipal;
import com.kcs.creditreport.service.ReportService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신용평가 리포트 API 컨트롤러 (API 명세서 §3.5, §3.6).
 *
 * <p>HTTP 관심사(쿼리 파라미터 파싱·검증)만 담당하고 비즈니스 로직은 {@link ReportService} 에
 * 위임한다. 현재 사용자는 {@code @AuthenticationPrincipal} 로 주입받아 권한 스코프로 사용한다.
 *
 * <p>잘못된 파라미터(정렬키·방향·등급 범위·날짜 형식·from&gt;to)는 컨트롤러에서 선검증해
 * {@code VALIDATION_ERROR}(400)로 응답한다(명세 §3.5 에러 케이스, §5.3·§5.4 규약).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 신용등급 허용 범위(명세 §5.3). */
    private static final int GRADE_MIN = 1;
    private static final int GRADE_MAX = 10;

    /** 페이지 크기 상한(명세 §5.1 권장). */
    private static final int SIZE_MAX = 100;

    /**
     * 리포트 목록 — 200 OK. 페이징·검색·필터·정렬을 서버에서 처리한다.
     *
     * <p>날짜(from/to)는 바인딩 실패 시 500 이 되지 않도록 {@code String} 으로 받아 직접 파싱해
     * 형식 오류를 400 으로 통일한다. 정렬키/방향도 화이트리스트로 검증한다.
     *
     * @param page  페이지 번호(1-based, 기본 1)
     * @param size  페이지당 항목 수(기본 10, 상한 100)
     * @param q     검색어(title 또는 institution 부분 일치)
     * @param grade 신용등급 필터(1~10)
     * @param from  발급일 시작(yyyy-MM-dd, 포함)
     * @param to    발급일 종료(yyyy-MM-dd, 포함)
     * @param sort  정렬 기준(issuedDate | creditScore, 기본 issuedDate)
     * @param order 정렬 방향(asc | desc, 기본 desc)
     */
    @GetMapping
    public PageResponse<ReportListItem> getReports(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "issuedDate") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        // 숫자 파라미터는 String 으로 받아 직접 파싱한다(바인딩 실패로 인한 500 회피 → 형식 오류는 400 통일)
        int validPage = validatePage(parseInt(page, "page"));
        int validSize = validateSize(parseInt(size, "size"));
        Integer gradeValue = parseNullableInt(grade, "grade");
        validateGrade(gradeValue);
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateDateRange(fromDate, toDate);
        SortKey sortKey = parseSortKey(sort);
        SortOrder sortOrder = parseSortOrder(order);

        ReportSearchCriteria criteria = new ReportSearchCriteria(
                q, gradeValue, fromDate, toDate, sortKey, sortOrder, validPage, validSize);

        return reportService.getReports(principal.userId(), criteria);
    }

    /**
     * 리포트 상세 — 200 OK. 조회 시 이력 자동 기록, 주민번호 마스킹.
     *
     * <p>본인 것만 조회(권한 격리). 미존재/타인 소유 ID 는 404 NOT_FOUND(서비스에서 처리).
     *
     * @param id 조회할 리포트 ID
     */
    @GetMapping("/{id}")
    public ReportDetail getReportDetail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id) {
        return reportService.getReportDetail(principal.userId(), id);
    }

    // ── 파라미터 검증 헬퍼 ───────────────────────────────────────────────

    /** 필수 숫자 파라미터 파싱(형식 오류 → 400). */
    private int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[" + field + "] 숫자 형식이 올바르지 않습니다.");
        }
    }

    /** 선택 숫자 파라미터 파싱(미지정 시 null, 형식 오류 → 400). */
    private Integer parseNullableInt(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseInt(value, field);
    }

    /** page 는 1 이상이어야 한다(1-based). */
    private int validatePage(int page) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "[page] 1 이상이어야 합니다.");
        }
        return page;
    }

    /** size 는 1~100 범위. */
    private int validateSize(int size) {
        if (size < 1 || size > SIZE_MAX) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[size] 1~" + SIZE_MAX + " 범위여야 합니다.");
        }
        return size;
    }

    /** grade 는 미지정이거나 1~10 범위(명세 §5.3). */
    private void validateGrade(Integer grade) {
        if (grade != null && (grade < GRADE_MIN || grade > GRADE_MAX)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[grade] " + GRADE_MIN + "~" + GRADE_MAX + " 범위여야 합니다.");
        }
    }

    /** 날짜 문자열을 LocalDate 로 파싱(미지정 시 null). 형식 오류는 400. */
    private LocalDate parseDate(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()); // ISO-8601 yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[" + field + "] 날짜 형식(yyyy-MM-dd)이 올바르지 않습니다.");
        }
    }

    /** from > to 면 400(명세 §5.3). */
    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[from,to] from 은 to 보다 이후일 수 없습니다.");
        }
    }

    /** 정렬 기준 화이트리스트(issuedDate|creditScore). 미허용 → 400. */
    private SortKey parseSortKey(String sort) {
        if ("issuedDate".equals(sort)) {
            return SortKey.ISSUED_DATE;
        }
        if ("creditScore".equals(sort)) {
            return SortKey.CREDIT_SCORE;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "[sort] issuedDate 또는 creditScore 만 허용됩니다.");
    }

    /** 정렬 방향 화이트리스트(asc|desc). 미허용 → 400. */
    private SortOrder parseSortOrder(String order) {
        if ("asc".equalsIgnoreCase(order)) {
            return SortOrder.ASC;
        }
        if ("desc".equalsIgnoreCase(order)) {
            return SortOrder.DESC;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "[order] asc 또는 desc 만 허용됩니다.");
    }
}
