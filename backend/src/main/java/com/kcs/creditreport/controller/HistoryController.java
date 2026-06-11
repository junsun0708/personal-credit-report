package com.kcs.creditreport.controller;

import com.kcs.creditreport.dto.common.PageResponse;
import com.kcs.creditreport.dto.report.HistoryItem;
import com.kcs.creditreport.exception.BusinessException;
import com.kcs.creditreport.exception.ErrorCode;
import com.kcs.creditreport.security.AuthPrincipal;
import com.kcs.creditreport.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 조회 이력 API 컨트롤러 (API 명세서 §3.7).
 *
 * <p>본인 이력만 최신순(viewed_at DESC)으로 페이징한다. 정렬은 고정이므로 sort/order 를 받지
 * 않으며, page/size 형식 오류만 검증(400)한다. 현재 사용자는 {@code @AuthenticationPrincipal}
 * 로 주입받아 권한 스코프로 사용한다.
 */
@RestController
@RequestMapping("/api/histories")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /** 페이지 크기 상한(명세 §5.1 권장). */
    private static final int SIZE_MAX = 100;

    /**
     * 내 조회 이력 — 200 OK. 최신순·페이징.
     *
     * @param page 페이지 번호(1-based, 기본 1)
     * @param size 페이지당 항목 수(기본 10, 상한 100)
     */
    @GetMapping
    public PageResponse<HistoryItem> getHistories(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "10") String size) {

        // 숫자 파라미터는 String 으로 받아 직접 파싱(바인딩 실패로 인한 500 회피 → 형식 오류는 400)
        int pageValue = parseInt(page, "page");
        int sizeValue = parseInt(size, "size");

        if (pageValue < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "[page] 1 이상이어야 합니다.");
        }
        if (sizeValue < 1 || sizeValue > SIZE_MAX) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[size] 1~" + SIZE_MAX + " 범위여야 합니다.");
        }

        return historyService.getHistories(principal.userId(), pageValue, sizeValue);
    }

    /** 숫자 파라미터 파싱(형식 오류 → 400). */
    private int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "[" + field + "] 숫자 형식이 올바르지 않습니다.");
        }
    }
}
