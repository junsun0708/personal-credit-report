/**
 * 리포트 목록 테이블 컬럼 정의(도메인 결합).
 * ui/Table의 columns로 주입한다(컬럼 하드코딩 금지 — 개발표준 §5.3).
 */
import type { TableColumn } from "@/components/ui/Table";
import { GradeBadge } from "@/components/domain/GradeBadge";
import { Button } from "@/components/ui/Button";
import { formatNumber } from "@/lib/utils/format";
import type { ReportListItem } from "@/types/report";

interface BuildReportColumnsArgs {
  /** [보기] 클릭 핸들러 */
  onView: (id: number) => void;
}

export function buildReportColumns({
  onView,
}: BuildReportColumnsArgs): TableColumn<ReportListItem>[] {
  return [
    {
      key: "title",
      header: "제목",
      cell: (r) => <span className="font-medium text-zinc-900">{r.title}</span>,
    },
    {
      key: "institution",
      header: "발급기관",
      cell: (r) => <span className="text-zinc-600">{r.institution}</span>,
    },
    {
      key: "creditScore",
      header: "점수",
      className: "text-right tabular-nums",
      headerClassName: "text-right",
      cell: (r) => formatNumber(r.creditScore),
    },
    {
      key: "creditGrade",
      header: "등급",
      cell: (r) => <GradeBadge grade={r.creditGrade} />,
    },
    {
      key: "issuedDate",
      header: "발급일",
      className: "tabular-nums text-zinc-600",
      cell: (r) => r.issuedDate,
    },
    {
      key: "action",
      header: "",
      className: "text-right",
      cell: (r) => (
        <Button
          variant="secondary"
          size="sm"
          onClick={(e) => {
            // 행 클릭과 중복 라우팅 방지
            e.stopPropagation();
            onView(r.id);
          }}
        >
          보기
        </Button>
      ),
    },
  ];
}
