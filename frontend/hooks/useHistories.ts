"use client";

/**
 * 조회 이력 목록 훅(TanStack Query 래핑).
 * staleTime: 0 으로 항상 최신을 보장(설계서 SCR-05).
 */
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getHistories } from "@/lib/api/histories";
import { queryKeys } from "@/lib/query/queryKeys";
import { DEFAULT_SIZE } from "@/lib/validators/reportParams";

export function useHistories(page: number, size: number = DEFAULT_SIZE) {
  return useQuery({
    queryKey: queryKeys.histories.list(page, size),
    queryFn: () => getHistories(page, size),
    staleTime: 0,
    placeholderData: keepPreviousData,
  });
}
