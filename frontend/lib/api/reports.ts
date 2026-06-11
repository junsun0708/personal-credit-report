/**
 * 리포트 API 호출 모듈.
 * 백엔드 /api/reports* 엔드포인트를 래핑한다.
 */
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { ReportDetail, ReportListItem, ReportListParams } from "@/types/report";

/**
 * 리포트 목록 조회 (서버 페이징·검색·필터·정렬).
 * 기본값(sort=issuedDate, order=desc)과 동일한 값도 명시 전송하여 서버 처리를 일관화한다.
 * 빈 값(q/grade/from/to)은 파라미터에서 생략한다.
 */
export async function getReports(
  params: ReportListParams,
): Promise<PageResponse<ReportListItem>> {
  const res = await apiClient.get<PageResponse<ReportListItem>>("/api/reports", {
    params: {
      page: params.page,
      size: params.size,
      sort: params.sort,
      order: params.order,
      ...(params.q ? { q: params.q } : {}),
      ...(params.grade != null ? { grade: params.grade } : {}),
      ...(params.from ? { from: params.from } : {}),
      ...(params.to ? { to: params.to } : {}),
    },
  });
  return res.data;
}

/**
 * 리포트 상세 조회.
 * 서버는 이 호출 시 조회 이력(ViewHistory)을 자동 기록한다. 404 NOT_FOUND.
 */
export async function getReportDetail(id: number): Promise<ReportDetail> {
  const res = await apiClient.get<ReportDetail>(`/api/reports/${id}`);
  return res.data;
}
