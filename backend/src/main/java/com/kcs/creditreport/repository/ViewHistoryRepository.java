package com.kcs.creditreport.repository;

import com.kcs.creditreport.domain.ViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 조회 이력 Repository.
 *
 * <p>본인 이력만 최신순(viewed_at DESC)으로 페이징 조회한다. (설계서 §3.3, §4)
 */
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    /** 본인 조회 이력 최신순 페이징. */
    Page<ViewHistory> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);
}
