/**
 * 로딩 플레이스홀더(도메인 무지).
 * count로 여러 줄을 반복 렌더한다.
 */
import { cn } from "@/lib/utils/cn";

interface SkeletonProps {
  /** 반복 개수. 기본 1 */
  count?: number;
  className?: string;
}

export function Skeleton({ count = 1, className }: SkeletonProps) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div
          // 순수 플레이스홀더라 index key 허용
          key={i}
          aria-hidden="true"
          className={cn("animate-pulse rounded-md bg-zinc-200", className)}
        />
      ))}
    </>
  );
}
