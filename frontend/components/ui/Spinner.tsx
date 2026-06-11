/**
 * 로딩 스피너(도메인 무지).
 * 버튼·인라인 로딩 등에 재사용한다.
 */
import { cn } from "@/lib/utils/cn";

interface SpinnerProps {
  /** 픽셀 크기(정사각). 기본 16 */
  size?: number;
  className?: string;
  /** 스크린리더용 라벨 */
  label?: string;
}

export function Spinner({ size = 16, className, label = "로딩 중" }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label={label}
      className={cn("inline-block animate-spin", className)}
      style={{ width: size, height: size }}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        className="h-full w-full"
        aria-hidden="true"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="3"
          className="opacity-25"
        />
        <path
          d="M22 12a10 10 0 0 1-10 10"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
        />
      </svg>
    </span>
  );
}
