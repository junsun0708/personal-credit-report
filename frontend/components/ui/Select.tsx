/**
 * 공통 드롭다운 셀렉트(도메인 무지).
 * options 배열을 받아 네이티브 select로 렌더(접근성·키보드 기본 지원).
 */
import { forwardRef, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

export interface SelectOption {
  label: string;
  value: string;
}

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "children"> {
  options: SelectOption[];
  /** 빈 선택 옵션 라벨(예: "전체"). 지정 시 빈 문자열 value로 추가 */
  placeholder?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { options, placeholder, className, ...rest },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        "h-10 rounded-md border border-zinc-300 bg-white px-3 text-sm text-zinc-900",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400",
        "disabled:bg-zinc-50 disabled:text-zinc-400",
        className,
      )}
      {...rest}
    >
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
});
