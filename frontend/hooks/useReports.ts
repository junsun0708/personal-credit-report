"use client";

/**
 * 리포트 목록 조회 훅(TanStack Query 래핑).
 * queryKey에 정규화된 필터 객체를 포함하여 조합별 캐싱(설계서 §5.2-5).
 */
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getReports } from "@/lib/api/reports";
import { queryKeys } from "@/lib/query/queryKeys";
import type { ReportListParams } from "@/types/report";

export function useReports(params: ReportListParams) {
  return useQuery({
    queryKey: queryKeys.reports.list(params),
    queryFn: () => getReports(params),
    // 페이지 전환 시 이전 데이터를 유지해 깜빡임을 줄인다.
    placeholderData: keepPreviousData,
  });
}
