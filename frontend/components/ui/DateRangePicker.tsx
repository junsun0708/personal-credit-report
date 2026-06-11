/**
 * 발급일 기간 선택(도메인 무지).
 * 두 개의 native date input(from~to)을 묶고 변경 시 onChange로 통지한다.
 */
import { cn } from "@/lib/utils/cn";

interface DateRangePickerProps {
  from?: string; // yyyy-MM-dd
  to?: string; // yyyy-MM-dd
  /** 변경된 from/to를 함께 전달(빈 값은 undefined) */
  onChange: (range: { from?: string; to?: string }) => void;
  className?: string;
}

export function DateRangePicker({
  from,
  to,
  onChange,
  className,
}: DateRangePickerProps) {
  const inputClass =
    "h-10 rounded-md border border-zinc-300 bg-white px-2 text-sm text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400";

  return (
    <div className={cn("flex items-center gap-1.5", className)}>
      <input
        type="date"
        aria-label="발급일 시작"
        value={from ?? ""}
        max={to || undefined}
        onChange={(e) => onChange({ from: e.target.value || undefined, to })}
        className={inputClass}
      />
      <span className="text-zinc-400">~</span>
      <input
        type="date"
        aria-label="발급일 종료"
        value={to ?? ""}
        min={from || undefined}
        onChange={(e) => onChange({ from, to: e.target.value || undefined })}
        className={inputClass}
      />
    </div>
  );
}
