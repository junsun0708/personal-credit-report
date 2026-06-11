package com.kcs.creditreport.service;

import com.kcs.creditreport.domain.ViewHistory;
import com.kcs.creditreport.dto.common.PageResponse;
import com.kcs.creditreport.dto.report.HistoryItem;
import com.kcs.creditreport.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조회 이력 도메인 비즈니스 로직 (API 명세서 §3.7).
 *
 * <p>본인 이력만 {@code viewed_at DESC}(최신순)로 페이징한다(권한 스코프 강제). 정렬은 항상
 * 최신순 고정이므로 sort/order 파라미터를 받지 않는다.
 *
 * <p>{@code readOnly} 트랜잭션 안에서 DTO 로 변환해, 연관 {@code CreditReport} 의 지연 로딩
 * 프로퍼티(title·institution·creditGrade) 접근이 안전하도록 한다.
 */
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ViewHistoryRepository viewHistoryRepository;

    /**
     * 본인 조회 이력 목록(최신순·페이징).
     *
     * @param currentUserId 현재 인증 사용자 ID(권한 스코프)
     * @param page          페이지 번호(1-based)
     * @param size          페이지당 항목 수
     */
    @Transactional(readOnly = true)
    public PageResponse<HistoryItem> getHistories(Long currentUserId, int page, int size) {
        // page 는 1-based 입력 → 0-based 로 변환. 정렬은 레포 메서드명에 고정(viewedAt DESC).
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<ViewHistory> histories =
                viewHistoryRepository.findByUserIdOrderByViewedAtDesc(currentUserId, pageable);
        return PageResponse.of(histories, HistoryItem::from);
    }
}
