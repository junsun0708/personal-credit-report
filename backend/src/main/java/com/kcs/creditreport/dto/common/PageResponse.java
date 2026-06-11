package com.kcs.creditreport.dto.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 공통 페이지 응답 래퍼 (API 명세서 §1.4).
 *
 * <pre>{@code
 * {
 *   "data": [],
 *   "page": 1,
 *   "size": 10,
 *   "totalElements": 0,
 *   "totalPages": 0
 * }
 * }</pre>
 *
 * <p>{@code page} 는 명세 규약상 <b>1-based</b> 로 노출한다. Spring {@link Page#getNumber()} 는
 * 0-based 이므로 변환 시 {@code +1} 한다.
 *
 * @param <T> 페이지 항목(응답용 DTO) 타입
 * @param data         현재 페이지의 항목 배열
 * @param page         현재 페이지 번호(1-based)
 * @param size         페이지당 항목 수
 * @param totalElements 필터 적용 후 전체 항목 수
 * @param totalPages   전체 페이지 수
 */
public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 이미 응답 DTO 로 매핑된 Spring {@link Page} 를 그대로 래핑한다.
     *
     * @param page 응답 DTO 가 담긴 페이지(엔티티 직접 직렬화 금지 — 호출부에서 DTO 로 변환 후 전달)
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1, // 0-based → 1-based
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * 엔티티 페이지를 응답 DTO 로 매핑하면서 래핑한다.
     *
     * <p>엔티티를 직접 직렬화하지 않도록, 호출부가 넘긴 {@code mapper} 로 각 항목을 DTO 로 변환한다.
     *
     * @param page   원본(엔티티) 페이지
     * @param mapper 엔티티 → 응답 DTO 변환 함수
     * @param <E>    원본 엔티티 타입
     * @param <T>    응답 DTO 타입
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1, // 0-based → 1-based
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
