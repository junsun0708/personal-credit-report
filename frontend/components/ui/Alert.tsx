/**
 * 인라인 알림 배너(도메인 무지).
 * 폼 공통 에러(401 등) 표시에 사용한다.
 */
import { type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type AlertType = "error" | "info" | "success" | "warning";

interface AlertProps {
  type?: AlertType;
  children: ReactNode;
  className?: string;
}

const TYPE_CLASSES: Record<AlertType, string> = {
  error: "bg-red-50 text-red-700 border-red-200",
  info: "bg-blue-50 text-blue-700 border-blue-200",
  success: "bg-green-50 text-green-700 border-green-200",
  warning: "bg-amber-50 text-amber-700 border-amber-200",
};

export function Alert({ type = "info", children, className }: AlertProps) {
  return (
    <div
      role={type === "error" ? "alert" : "status"}
      className={cn(
        "rounded-md border px-3 py-2 text-sm",
        TYPE_CLASSES[type],
        className,
      )}
    >
      {children}
    </div>
  );
}
