"use client";

/**
 * SCR-03 · 리포트 목록 (`/reports`).
 *
 * - URL searchParams를 SSOT로 검색·필터·정렬·페이징 동기화(useReportListParams).
 * - 서버 페이징(10건). 클라이언트 필터링 금지.
 * - 로딩(Skeleton 10줄)/에러(ErrorState+재시도)/빈상태(EmptyState) 일관 처리.
 */
import { Suspense, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useReportListParams } from "@/hooks/useReportListParams";
import { useReports } from "@/hooks/useReports";
import { ReportFilterBar } from "@/components/domain/ReportFilterBar";
import { buildReportColumns } from "@/components/domain/reportColumns";
import { Table } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { Skeleton } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";

function ReportsView() {
  const router = useRouter();
  const { params, patch, setSearch, setPage, reset } = useReportListParams();
  const { data, isPending, isError, refetch, isFetching } = useReports(params);

  const columns = useMemo(
    () => buildReportColumns({ onView: (id) => router.push(`/reports/${id}`) }),
    [router],
  );

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-xl font-bold text-zinc-900">신용평가 리포트 목록</h1>

      <ReportFilterBar
        params={params}
        onChange={patch}
        onSearch={setSearch}
        onReset={reset}
      />

      {/* 본문: 상태별 분기 */}
      {isPending ? (
        <div className="flex flex-col gap-2 rounded-lg border border-zinc-200 bg-white p-4">
          <Skeleton count={10} className="h-10 w-full" />
        </div>
      ) : isError ? (
        <ErrorState
          message="리포트를 불러오지 못했습니다."
          onRetry={() => void refetch()}
        />
      ) : data.data.length === 0 ? (
        <EmptyState
          message="조건에 맞는 리포트가 없습니다."
          action={
            <Button variant="secondary" size="sm" onClick={reset}>
              필터 초기화
            </Button>
          }
        />
      ) : (
        <div className={isFetching ? "opacity-70 transition-opacity" : undefined}>
          <Table
            columns={columns}
            data={data.data}
            rowKey={(r) => r.id}
            onRowClick={(r) => router.push(`/reports/${r.id}`)}
          />
          <div className="mt-5">
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={setPage}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default function ReportsPage() {
  // useSearchParams(useReportListParams 내부) 사용 → Suspense 경계
  return (
    <Suspense fallback={null}>
      <ReportsView />
    </Suspense>
  );
}
