/**
 * TanStack Query 키 팩토리.
 * 키 구조를 한 곳에 모아 invalidate·캐싱 조합을 일관화한다.
 */
import type { ReportListParams } from "@/types/report";

export const queryKeys = {
  reports: {
    /** 목록: 필터 조합별 캐싱 (화면 설계서 §5.2-5) */
    list: (params: ReportListParams) => ["reports", "list", params] as const,
    /** 상세 */
    detail: (id: number) => ["reports", "detail", id] as const,
  },
  histories: {
    /** 이력 목록(페이지별) */
    list: (page: number, size: number) => ["histories", "list", page, size] as const,
    /** 전체 이력 무효화용 prefix */
    all: ["histories"] as const,
  },
} as const;
