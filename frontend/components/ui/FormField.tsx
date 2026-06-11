/**
 * 폼 필드 래퍼(도메인 무지).
 * 라벨 + 필드(children) + 헬프텍스트/에러를 일관 레이아웃으로 묶는다.
 */
import { type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface FormFieldProps {
  label: string;
  /** 연결할 input의 id (label htmlFor) */
  htmlFor?: string;
  /** 필드 하단 보조 설명(에러 없을 때 표시) */
  description?: ReactNode;
  /** 에러 메시지(있으면 description 대신 표시) */
  error?: string;
  required?: boolean;
  children: ReactNode;
  className?: string;
}

export function FormField({
  label,
  htmlFor,
  description,
  error,
  required = false,
  children,
  className,
}: FormFieldProps) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={htmlFor} className="text-sm font-medium text-zinc-800">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </label>
      {children}
      {error ? (
        <p className="text-xs text-red-600" role="alert">
          ⚠ {error}
        </p>
      ) : description ? (
        <div className="text-xs text-zinc-500">{description}</div>
      ) : null}
    </div>
  );
}
