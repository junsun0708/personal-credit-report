/**
 * 신용평가 리포트·조회 이력 도메인 타입.
 * 백엔드 /api/reports, /api/histories 계약과 1:1 대응한다.
 */

/** 목록 행 아이템 (GET /api/reports) */
export interface ReportListItem {
  id: number;
  title: string;
  institution: string;
  creditScore: number;
  creditGrade: number;
  issuedDate: string; // yyyy-MM-dd
}

/** 리포트 상세 (GET /api/reports/{id}) */
export interface ReportDetail {
  id: number;
  title: string;
  institution: string;
  creditScore: number;
  creditGrade: number;
  issuedDate: string; // yyyy-MM-dd
  maskedSsn: string;
  reportSummary: string;
  delinquencyCount: number;
  totalDebt: number;
  creditCardCount: number;
  loanCount: number;
  createdAt: string;
}

/** 조회 이력 행 아이템 (GET /api/histories) */
export interface HistoryItem {
  id: number;
  reportId: number;
  reportTitle: string;
  institution: string;
  creditGrade: number;
  viewedAt: string; // ISO datetime
}

/** 정렬 기준 */
export type ReportSortKey = "issuedDate" | "creditScore";

/** 정렬 방향 */
export type SortOrder = "asc" | "desc";

/**
 * 리포트 목록 조회 파라미터 (URL searchParams 파생 정규화 결과).
 * 기본값이 적용된 정규화 형태이며, queryKey·API 호출에 그대로 사용한다.
 */
export interface ReportListParams {
  page: number; // 1-base
  size: number; // 기본 10
  q?: string;
  grade?: number; // 1~10
  from?: string; // yyyy-MM-dd
  to?: string; // yyyy-MM-dd
  sort: ReportSortKey;
  order: SortOrder;
}
