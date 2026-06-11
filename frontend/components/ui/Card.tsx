/**
 * 정보 카드 컨테이너(도메인 무지).
 * 선택적 title 헤더 + 본문(children).
 */
import { type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

interface CardProps {
  title?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function Card({ title, children, className }: CardProps) {
  return (
    <section
      className={cn(
        "rounded-lg border border-zinc-200 bg-white p-5 shadow-sm",
        className,
      )}
    >
      {title && (
        <h2 className="mb-4 text-sm font-semibold text-zinc-500">{title}</h2>
      )}
      {children}
    </section>
  );
}
