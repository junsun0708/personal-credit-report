/**
 * 에러 상태(도메인 무지).
 * 메시지 + 재시도 버튼을 표시한다.
 */
import { Button } from "@/components/ui/Button";

interface ErrorStateProps {
  message?: string;
  /** 재시도 콜백(주로 Query refetch) */
  onRetry?: () => void;
}

export function ErrorState({
  message = "데이터를 불러오는 중 문제가 발생했습니다.",
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-lg border border-red-200 bg-red-50 px-6 py-16 text-center">
      <p className="text-sm font-medium text-red-700">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  );
}
