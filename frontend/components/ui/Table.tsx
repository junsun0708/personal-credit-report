/**
 * 조합형 테이블(도메인 무지).
 *
 * - columns로 헤더·정렬·셀 렌더러를 주입한다(컬럼 하드코딩 금지 — 개발표준 §5.3).
 * - 제네릭 T로 어떤 행 데이터에도 재사용 가능.
 * - 로딩/빈상태는 상위에서 처리하고, 본 컴포넌트는 data 렌더에 집중한다.
 */
import { type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

export interface TableColumn<T> {
  /** 컬럼 식별 키 */
  key: string;
  /** 헤더 라벨 */
  header: ReactNode;
  /** 셀 렌더러: 행 데이터를 받아 내용을 반환 */
  cell: (row: T) => ReactNode;
  /** 셀/헤더 정렬 클래스 등 */
  className?: string;
  /** 헤더 전용 추가 클래스 */
  headerClassName?: string;
}

interface TableProps<T> {
  columns: TableColumn<T>[];
  data: T[];
  /** 각 행의 안정적인 key 추출 */
  rowKey: (row: T) => string | number;
  /** 행 클릭 핸들러(선택). 지정 시 행에 hover/cursor 스타일 부여 */
  onRowClick?: (row: T) => void;
  className?: string;
}

export function Table<T>({
  columns,
  data,
  rowKey,
  onRowClick,
  className,
}: TableProps<T>) {
  return (
    <div className={cn("w-full overflow-x-auto rounded-lg border border-zinc-200", className)}>
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-zinc-200 bg-zinc-50">
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className={cn(
                  "px-4 py-3 text-left font-semibold text-zinc-600",
                  col.headerClassName,
                  col.className,
                )}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={cn(
                "border-b border-zinc-100 last:border-0",
                onRowClick && "cursor-pointer hover:bg-zinc-50",
              )}
            >
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={cn("px-4 py-3 text-zinc-800", col.className)}
                >
                  {col.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
