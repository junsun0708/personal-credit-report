/**
 * 공통 텍스트 입력(도메인 무지).
 * error 상태 시 테두리 강조 + aria-invalid.
 */
import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /** 에러 상태 여부(테두리 강조) */
  error?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { error = false, className, ...rest },
  ref,
) {
  return (
    <input
      ref={ref}
      aria-invalid={error || undefined}
      className={cn(
        "h-10 w-full rounded-md border bg-white px-3 text-sm text-zinc-900",
        "placeholder:text-zinc-400",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-0",
        error
          ? "border-red-500 focus-visible:ring-red-400"
          : "border-zinc-300 focus-visible:ring-blue-400",
        "disabled:bg-zinc-50 disabled:text-zinc-400",
        className,
      )}
      {...rest}
    />
  );
});
