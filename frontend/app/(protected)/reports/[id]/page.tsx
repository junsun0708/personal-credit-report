"use client";

/**
 * SCR-04 · 리포트 상세 (`/reports/[id]`).
 *
 * - 진입 시 GET /api/reports/{id} → 서버가 조회 이력 자동 기록, 성공 시 이력 invalidate(훅 내부).
 * - 점수 게이지·등급 배지·기본정보·마스킹·상세 항목 표시.
 * - 404는 notFound() 폴백, 5xx는 ErrorState 재시도.
 */
import { use, useEffect } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { useReportDetail } from "@/hooks/useReportDetail";
import { extractErrorCode } from "@/lib/api/client";
import { CreditScoreGauge } from "@/components/domain/CreditScoreGauge";
import { GradeBadge } from "@/components/domain/GradeBadge";
import { MaskedField } from "@/components/domain/MaskedField";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { getGradeLabel } from "@/lib/utils/grade";
import { formatCurrency, formatDateTime, formatNumber } from "@/lib/utils/format";

interface ReportDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ReportDetailPage({ params }: ReportDetailPageProps) {
  // Next.js 15+ : params는 Promise → use()로 언래핑
  const { id } = use(params);
  const numericId = Number(id);

  const { data, isPending, isError, error, refetch } = useReportDetail(numericId);

  // 404 NOT_FOUND → 폴백
  useEffect(() => {
    if (isError && extractErrorCode(error) === "NOT_FOUND") {
      notFound();
    }
  }, [isError, error]);

  // 잘못된 id 형식 즉시 폴백
  if (!Number.isInteger(numericId) || numericId <= 0) {
    notFound();
  }

  return (
    <div className="flex flex-col gap-5">
      <Link
        href="/reports"
        className="inline-flex items-center gap-1 text-sm text-zinc-500 hover:text-zinc-800"
      >
        ‹ 목록으로
      </Link>

      {isPending ? (
        <div className="flex flex-col gap-4">
          <Skeleton className="h-8 w-2/3" />
          <Skeleton className="h-4 w-1/3" />
          <div className="grid gap-4 lg:grid-cols-2">
            <Skeleton className="h-56 w-full" />
            <Skeleton className="h-56 w-full" />
          </div>
        </div>
      ) : isError ? (
        <ErrorState
          message="리포트를 불러오지 못했습니다."
          onRetry={() => void refetch()}
        />
      ) : (
        <>
          <header>
            <h1 className="text-2xl font-bold text-zinc-900">{data.title}</h1>
            <p className="mt-1 text-sm text-zinc-500">
              발급기관: {data.institution} &nbsp;|&nbsp; 발급일: {data.issuedDate}
            </p>
          </header>

          {/* 점수 / 등급 — 데스크탑 2열 */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="신용점수">
              <div className="flex justify-center py-2">
                <CreditScoreGauge
                  value={data.creditScore}
                  max={1000}
                  grade={data.creditGrade}
                />
              </div>
            </Card>

            <Card title="신용등급">
              <div className="flex flex-col items-center justify-center gap-3 py-8">
                <GradeBadge grade={data.creditGrade} withLabel className="px-4 py-1.5 text-base" />
                <p className="text-sm text-zinc-500">
                  {data.creditGrade}등급 · {getGradeLabel(data.creditGrade)}
                </p>
              </div>
            </Card>
          </div>

          {/* 기본 정보 */}
          <Card title="기본 정보">
            <div className="divide-y divide-zinc-100">
              <MaskedField label="주민등록번호" maskedValue={data.maskedSsn} />
              <div className="flex items-center justify-between gap-4 py-2">
                <span className="text-sm text-zinc-500">발급기관</span>
                <span className="text-sm text-zinc-900">{data.institution}</span>
              </div>
              <div className="flex items-center justify-between gap-4 py-2">
                <span className="text-sm text-zinc-500">발급일자</span>
                <span className="text-sm text-zinc-900">{data.issuedDate}</span>
              </div>
              <div className="flex items-center justify-between gap-4 py-2">
                <span className="text-sm text-zinc-500">기록일시</span>
                <span className="text-sm text-zinc-900">{formatDateTime(data.createdAt)}</span>
              </div>
            </div>
          </Card>

          {/* 상세 요약 항목 */}
          <Card title="신용 상세">
            {data.reportSummary && (
              <p className="mb-4 whitespace-pre-line text-sm leading-relaxed text-zinc-700">
                {data.reportSummary}
              </p>
            )}
            <dl className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <DetailStat label="연체 건수" value={`${formatNumber(data.delinquencyCount)}건`} />
              <DetailStat label="총 부채" value={formatCurrency(data.totalDebt)} />
              <DetailStat label="신용카드" value={`${formatNumber(data.creditCardCount)}개`} />
              <DetailStat label="대출 건수" value={`${formatNumber(data.loanCount)}건`} />
            </dl>
          </Card>
        </>
      )}
    </div>
  );
}

interface DetailStatProps {
  label: string;
  value: string;
}

function DetailStat({ label, value }: DetailStatProps) {
  return (
    <div className="rounded-lg bg-zinc-50 p-3">
      <dt className="text-xs text-zinc-500">{label}</dt>
      <dd className="mt-1 text-base font-semibold text-zinc-900">{value}</dd>
    </div>
  );
}
