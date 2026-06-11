/**
 * 서버 페이징 UI(도메인 무지).
 *
 * - 1-base page. totalPages 기반으로 윈도우(최대 5개) 번호를 노출한다.
 * - 이전/다음 + 번호 버튼. 현재 페이지 강조.
 * - 총 건수·현재/전체 페이지 요약 표시.
 */
import { cn } from "@/lib/utils/cn";

interface PaginationProps {
  /** 현재 페이지(1-base) */
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}

/** 현재 페이지 주변 최대 5개 번호 윈도우를 계산 */
function buildPageWindow(page: number, totalPages: number): number[] {
  const WINDOW = 5;
  if (totalPages <= WINDOW) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  let start = Math.max(1, page - Math.floor(WINDOW / 2));
  const end = Math.min(totalPages, start + WINDOW - 1);
  start = Math.max(1, end - WINDOW + 1);
  return Array.from({ length: end - start + 1 }, (_, i) => start + i);
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: PaginationProps) {
  if (totalPages <= 0) return null;

  const pages = buildPageWindow(page, totalPages);
  const canPrev = page > 1;
  const canNext = page < totalPages;

  const btnBase =
    "inline-flex h-8 min-w-8 items-center justify-center rounded-md border px-2 text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-40";

  return (
    <nav
      className="flex flex-wrap items-center justify-center gap-2"
      aria-label="페이지네이션"
    >
      <button
        type="button"
        className={cn(btnBase, "border-zinc-300 text-zinc-600 hover:bg-zinc-100")}
        disabled={!canPrev}
        onClick={() => onChange(page - 1)}
        aria-label="이전 페이지"
      >
        ◀
      </button>

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          aria-current={p === page ? "page" : undefined}
          onClick={() => onChange(p)}
          className={cn(
            btnBase,
            p === page
              ? "border-blue-600 bg-blue-600 text-white"
              : "border-zinc-300 text-zinc-700 hover:bg-zinc-100",
          )}
        >
          {p}
        </button>
      ))}

      <button
        type="button"
        className={cn(btnBase, "border-zinc-300 text-zinc-600 hover:bg-zinc-100")}
        disabled={!canNext}
        onClick={() => onChange(page + 1)}
        aria-label="다음 페이지"
      >
        ▶
      </button>

      <span className="ml-2 text-xs text-zinc-500">
        총 {totalElements.toLocaleString("ko-KR")}건 · {page}/{totalPages}p
      </span>
    </nav>
  );
}
