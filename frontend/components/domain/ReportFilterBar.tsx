"use client";

/**
 * 리포트 검색·필터·정렬 통합 바(도메인 결합).
 *
 * - params(정규화된 현재 상태)를 받아 controlled로 렌더.
 * - 검색어는 디바운스를 위해 로컬 임시 state로만 보유하고(설계서 §5.2),
 *   300ms 후 onSearch로 통지한다. 나머지(등급·기간·정렬)는 즉시 onChange.
 * - URL 동기화·page 리셋 규칙은 상위(useReportListParams)가 담당한다.
 */
import { useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { DateRangePicker } from "@/components/ui/DateRangePicker";
import type { ReportListParams, ReportSortKey, SortOrder } from "@/types/report";

interface ReportFilterBarProps {
  params: ReportListParams;
  /** 등급/기간/정렬 등 즉시 반영 항목 변경 */
  onChange: (patch: Partial<ReportListParams>) => void;
  /** 디바운스된 검색어 변경 */
  onSearch: (q: string | undefined) => void;
  /** 필터 전체 초기화 */
  onReset: () => void;
}

const GRADE_OPTIONS = Array.from({ length: 10 }, (_, i) => ({
  label: `${i + 1}등급`,
  value: String(i + 1),
}));

const SORT_OPTIONS = [
  { label: "발급일", value: "issuedDate" },
  { label: "신용점수", value: "creditScore" },
];

const ORDER_OPTIONS = [
  { label: "내림차순", value: "desc" },
  { label: "오름차순", value: "asc" },
];

export function ReportFilterBar({
  params,
  onChange,
  onSearch,
  onReset,
}: ReportFilterBarProps) {
  // 검색 입력 임시 보유(디바운스 전용). URL의 q와 동기화.
  const [searchText, setSearchText] = useState(params.q ?? "");
  // 외부 q 변화를 감지하기 위한 이전 값 추적(React 권장 "렌더 중 파생 상태 조정" 패턴).
  const [prevQ, setPrevQ] = useState(params.q);
  const timerRef = useRef<number | null>(null);

  // 외부에서 q가 바뀌면(초기화·뒤로가기 등) 입력도 맞춘다.
  if (prevQ !== params.q) {
    setPrevQ(params.q);
    setSearchText(params.q ?? "");
  }

  // 언마운트 시 타이머 정리
  useEffect(() => {
    return () => {
      if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    };
  }, []);

  const handleSearchChange = (value: string): void => {
    setSearchText(value);
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      const trimmed = value.trim();
      onSearch(trimmed.length > 0 ? trimmed : undefined);
    }, 300);
  };

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-zinc-200 bg-white p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <Input
          type="search"
          placeholder="🔍 제목·발급기관 검색"
          value={searchText}
          onChange={(e) => handleSearchChange(e.target.value)}
          className="sm:w-64"
          aria-label="제목·발급기관 검색"
        />

        <div className="flex items-center gap-2">
          <span className="text-sm text-zinc-500">등급</span>
          <Select
            placeholder="전체"
            options={GRADE_OPTIONS}
            value={params.grade != null ? String(params.grade) : ""}
            onChange={(e) =>
              onChange({ grade: e.target.value ? Number(e.target.value) : undefined })
            }
            aria-label="신용등급 필터"
          />
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm text-zinc-500">기간</span>
          <DateRangePicker
            from={params.from}
            to={params.to}
            onChange={(range) => onChange({ from: range.from, to: range.to })}
          />
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm text-zinc-500">정렬</span>
          <Select
            options={SORT_OPTIONS}
            value={params.sort}
            onChange={(e) => onChange({ sort: e.target.value as ReportSortKey })}
            aria-label="정렬 기준"
          />
          <Select
            options={ORDER_OPTIONS}
            value={params.order}
            onChange={(e) => onChange({ order: e.target.value as SortOrder })}
            aria-label="정렬 방향"
          />
        </div>

        <Button variant="ghost" size="sm" onClick={onReset}>
          필터 초기화
        </Button>
      </div>
    </div>
  );
}
