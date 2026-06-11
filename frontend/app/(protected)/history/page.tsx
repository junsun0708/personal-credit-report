"use client";

/**
 * SCR-05 · 조회 이력 (`/history`).
 *
 * - 최신순 + 페이지네이션(10). 동일 리포트 중복 조회 시점 모두 표시.
 * - page만 URL 동기화(검색·필터 없음). staleTime 0으로 항상 최신.
 * - 로딩/에러/빈상태 일관 처리.
 */
import { Suspense, useMemo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useHistories } from "@/hooks/useHistories";
import { buildHistoryColumns } from "@/components/domain/historyColumns";
import { Table } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { Skeleton } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";

function HistoryView() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // page만 URL 동기화(1-base, 유효성 가드)
  const pageRaw = Number(searchParams.get("page"));
  const page = Number.isInteger(pageRaw) && pageRaw >= 1 ? pageRaw : 1;

  const { data, isPending, isError, refetch, isFetching } = useHistories(page);

  const columns = useMemo(
    () => buildHistoryColumns({ onView: (reportId) => router.push(`/reports/${reportId}`) }),
    [router],
  );

  const handlePageChange = (next: number): void => {
    const sp = new URLSearchParams(searchParams.toString());
    if (next <= 1) sp.delete("page");
    else sp.set("page", String(next));
    const qs = sp.toString();
    router.replace(qs ? `/history?${qs}` : "/history", { scroll: false });
  };

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-xl font-bold text-zinc-900">조회 이력</h1>

      {isPending ? (
        <div className="flex flex-col gap-2 rounded-lg border border-zinc-200 bg-white p-4">
          <Skeleton count={10} className="h-10 w-full" />
        </div>
      ) : isError ? (
        <ErrorState
          message="조회 이력을 불러오지 못했습니다."
          onRetry={() => void refetch()}
        />
      ) : data.data.length === 0 ? (
        <EmptyState
          message="아직 조회한 리포트가 없습니다."
          action={
            <Button variant="secondary" size="sm" onClick={() => router.push("/reports")}>
              리포트 보러 가기
            </Button>
          }
        />
      ) : (
        <div className={isFetching ? "opacity-70 transition-opacity" : undefined}>
          <Table columns={columns} data={data.data} rowKey={(h) => h.id} />
          <div className="mt-5">
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={handlePageChange}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default function HistoryPage() {
  return (
    <Suspense fallback={null}>
      <HistoryView />
    </Suspense>
  );
}
