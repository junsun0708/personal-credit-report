/**
 * 조회 이력 테이블 컬럼 정의(도메인 결합).
 * 동일 리포트 중복 시점도 각각 한 행으로 표시(설계서 SCR-05).
 */
import type { TableColumn } from "@/components/ui/Table";
import { GradeBadge } from "@/components/domain/GradeBadge";
import { Button } from "@/components/ui/Button";
import { formatDateTime } from "@/lib/utils/format";
import type { HistoryItem } from "@/types/report";

interface BuildHistoryColumnsArgs {
  onView: (reportId: number) => void;
}

export function buildHistoryColumns({
  onView,
}: BuildHistoryColumnsArgs): TableColumn<HistoryItem>[] {
  return [
    {
      key: "viewedAt",
      header: "조회일시",
      className: "tabular-nums text-zinc-600",
      cell: (h) => formatDateTime(h.viewedAt),
    },
    {
      key: "reportTitle",
      header: "리포트 제목",
      cell: (h) => <span className="font-medium text-zinc-900">{h.reportTitle}</span>,
    },
    {
      key: "institution",
      header: "발급기관",
      cell: (h) => <span className="text-zinc-600">{h.institution}</span>,
    },
    {
      key: "creditGrade",
      header: "등급",
      cell: (h) => <GradeBadge grade={h.creditGrade} />,
    },
    {
      key: "action",
      header: "",
      className: "text-right",
      cell: (h) => (
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onView(h.reportId)}
        >
          보기
        </Button>
      ),
    },
  ];
}
