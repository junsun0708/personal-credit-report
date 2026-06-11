/**
 * 빈 상태(도메인 무지).
 * 메시지 + 선택적 액션 영역을 표시한다.
 */
import { type ReactNode } from "react";

interface EmptyStateProps {
  message: string;
  description?: string;
  /** 액션 버튼 등 */
  action?: ReactNode;
}

export function EmptyState({ message, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-zinc-300 bg-zinc-50 px-6 py-16 text-center">
      <p className="text-sm font-medium text-zinc-700">{message}</p>
      {description && <p className="text-xs text-zinc-500">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
