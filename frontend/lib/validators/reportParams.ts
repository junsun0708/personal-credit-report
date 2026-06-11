/**
 * 리포트 목록 URL searchParams ↔ ReportListParams 정규화.
 *
 * 화면 설계서 §5 동기화 규칙을 구현한다:
 * - 잘못된 파라미터(음수 page, 비날짜 등)는 기본값으로 정규화(유효성 가드).
 * - 기본값(sort=issuedDate, order=desc, page=1, size=10)은 URL에서 생략(깔끔한 공유 URL).
 * - searchParams가 목록 상태의 단일 진실 공급원(SSOT).
 */
import { z } from "zod";
import type { ReportListParams } from "@/types/report";

/** 기본값 상수 */
export const DEFAULT_PAGE = 1;
export const DEFAULT_SIZE = 10;
export const DEFAULT_SORT = "issuedDate" as const;
export const DEFAULT_ORDER = "desc" as const;

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** yyyy-MM-dd 형식이며 실제 유효한 날짜인지 검사 */
function isValidDate(value: string): boolean {
  if (!DATE_PATTERN.test(value)) return false;
  const date = new Date(value);
  return !Number.isNaN(date.getTime());
}

/**
 * URLSearchParams(또는 raw record)를 정규화된 ReportListParams로 변환.
 * 모든 잘못된 입력은 안전한 기본값으로 흡수한다(throw 없음).
 */
export function parseReportListParams(
  search: URLSearchParams | Record<string, string | undefined>,
): ReportListParams {
  const get = (key: string): string | undefined => {
    if (search instanceof URLSearchParams) {
      return search.get(key) ?? undefined;
    }
    return search[key];
  };

  // page: 1 이상 정수, 아니면 1
  const pageRaw = Number(get("page"));
  const page =
    Number.isInteger(pageRaw) && pageRaw >= 1 ? pageRaw : DEFAULT_PAGE;

  // sort/order: 허용값만, 아니면 기본
  const sortRaw = get("sort");
  const sort: ReportListParams["sort"] =
    sortRaw === "creditScore" || sortRaw === "issuedDate" ? sortRaw : DEFAULT_SORT;

  const orderRaw = get("order");
  const order: ReportListParams["order"] =
    orderRaw === "asc" || orderRaw === "desc" ? orderRaw : DEFAULT_ORDER;

  // q: 트림 후 비어있지 않으면 채택
  const qRaw = get("q")?.trim();
  const q = qRaw && qRaw.length > 0 ? qRaw : undefined;

  // grade: 1~10 정수만
  const gradeRaw = Number(get("grade"));
  const grade =
    Number.isInteger(gradeRaw) && gradeRaw >= 1 && gradeRaw <= 10
      ? gradeRaw
      : undefined;

  // from/to: 유효한 yyyy-MM-dd만
  const fromRaw = get("from");
  const from = fromRaw && isValidDate(fromRaw) ? fromRaw : undefined;

  const toRaw = get("to");
  let to = toRaw && isValidDate(toRaw) ? toRaw : undefined;

  // from > to 인 비정상 구간은 to를 무효화(방어적)
  if (from && to && from > to) {
    to = undefined;
  }

  return { page, size: DEFAULT_SIZE, q, grade, from, to, sort, order };
}

/**
 * ReportListParams를 URL 쿼리스트링으로 직렬화.
 * 기본값과 동일한 항목은 생략하여 깔끔한 공유 URL을 만든다.
 */
export function serializeReportListParams(params: ReportListParams): string {
  const sp = new URLSearchParams();

  if (params.page !== DEFAULT_PAGE) sp.set("page", String(params.page));
  if (params.q) sp.set("q", params.q);
  if (params.grade != null) sp.set("grade", String(params.grade));
  if (params.from) sp.set("from", params.from);
  if (params.to) sp.set("to", params.to);
  if (params.sort !== DEFAULT_SORT) sp.set("sort", params.sort);
  if (params.order !== DEFAULT_ORDER) sp.set("order", params.order);
  // size는 고정값이므로 URL 미노출

  return sp.toString();
}

/** zod 스키마 형태(외부에서 타입 추론·재사용이 필요할 때) */
export const reportListParamsSchema = z.object({
  page: z.number().int().min(1),
  size: z.number().int().min(1),
  q: z.string().optional(),
  grade: z.number().int().min(1).max(10).optional(),
  from: z.string().optional(),
  to: z.string().optional(),
  sort: z.enum(["issuedDate", "creditScore"]),
  order: z.enum(["asc", "desc"]),
});
