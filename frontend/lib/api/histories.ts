/**
 * 조회 이력 API 호출 모듈.
 * 백엔드 /api/histories 엔드포인트를 래핑한다.
 */
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { HistoryItem } from "@/types/report";

/** 조회 이력 목록(최신순, 서버 페이징). 검색·필터 없음. */
export async function getHistories(
  page: number,
  size: number,
): Promise<PageResponse<HistoryItem>> {
  const res = await apiClient.get<PageResponse<HistoryItem>>("/api/histories", {
    params: { page, size },
  });
  return res.data;
}
