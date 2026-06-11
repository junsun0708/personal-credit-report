"use client";

/**
 * 리포트 목록 URL 동기화 훅(설계서 §5 구현).
 *
 * 동작:
 * - useSearchParams()를 SSOT로 삼아 정규화된 ReportListParams를 도출.
 * - patch/setPage/reset 호출 시 router.replace로 URL에 반영(히스토리 오염 방지).
 * - page 리셋 규칙: q/grade/from/to/sort/order 변경 시 page=1, page 변경만 단독.
 * - 기본값은 직렬화 단계에서 생략(깔끔한 공유 URL).
 */
import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  parseReportListParams,
  serializeReportListParams,
  DEFAULT_PAGE,
  DEFAULT_SIZE,
  DEFAULT_SORT,
  DEFAULT_ORDER,
} from "@/lib/validators/reportParams";
import type { ReportListParams } from "@/types/report";

interface UseReportListParamsResult {
  /** 정규화된 현재 목록 파라미터(SSOT 파생) */
  params: ReportListParams;
  /** 필터/정렬 등 변경 — page는 1로 리셋 */
  patch: (next: Partial<ReportListParams>) => void;
  /** 검색어 변경(디바운스된 값) — page 1로 리셋 */
  setSearch: (q: string | undefined) => void;
  /** 페이지 변경 — 다른 조건 유지 */
  setPage: (page: number) => void;
  /** 전체 초기화 — /reports 로 복귀 */
  reset: () => void;
}

export function useReportListParams(): UseReportListParamsResult {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  // searchParams.toString()을 의존성으로 사용해 안정적으로 재계산
  const search = searchParams.toString();

  const params = useMemo(
    () => parseReportListParams(new URLSearchParams(search)),
    [search],
  );

  /** 내부 공통: 다음 params를 URL에 replace */
  const replaceWith = useCallback(
    (next: ReportListParams) => {
      const qs = serializeReportListParams(next);
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [router, pathname],
  );

  const patch = useCallback(
    (next: Partial<ReportListParams>) => {
      // 필터/정렬 변경 시 page=1 리셋
      replaceWith({ ...params, ...next, page: DEFAULT_PAGE });
    },
    [params, replaceWith],
  );

  const setSearch = useCallback(
    (q: string | undefined) => {
      // 동일 검색어면 무의미한 replace 방지
      if ((params.q ?? "") === (q ?? "")) return;
      replaceWith({ ...params, q, page: DEFAULT_PAGE });
    },
    [params, replaceWith],
  );

  const setPage = useCallback(
    (page: number) => {
      replaceWith({ ...params, page });
    },
    [params, replaceWith],
  );

  const reset = useCallback(() => {
    replaceWith({
      page: DEFAULT_PAGE,
      size: DEFAULT_SIZE,
      sort: DEFAULT_SORT,
      order: DEFAULT_ORDER,
    });
  }, [replaceWith]);

  return { params, patch, setSearch, setPage, reset };
}
