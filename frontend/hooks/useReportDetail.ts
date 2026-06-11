"use client";

/**
 * 리포트 상세 조회 훅(TanStack Query 래핑).
 *
 * - 상세 진입 시 서버가 조회 이력을 자동 기록하므로, 조회 성공 후
 *   이력 쿼리를 invalidate 한다(설계서 SCR-04).
 * - staleTime: 0 으로 진입마다 신선 조회를 보장(매 진입 = 새 이력 기록).
 */
import { useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getReportDetail } from "@/lib/api/reports";
import { queryKeys } from "@/lib/query/queryKeys";

export function useReportDetail(id: number) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: queryKeys.reports.detail(id),
    queryFn: () => getReportDetail(id),
    enabled: Number.isInteger(id) && id > 0,
    staleTime: 0,
    retry: 1,
  });

  // 조회 성공(= 서버에 이력 기록됨) 시 이력 목록 무효화
  useEffect(() => {
    if (query.isSuccess) {
      void queryClient.invalidateQueries({ queryKey: queryKeys.histories.all });
    }
  }, [query.isSuccess, query.data, queryClient]);

  return query;
}
