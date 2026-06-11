/**
 * 공통 배지(도메인 무지).
 * tone(색)과 children만 받는다. 도메인 색 매핑은 상위(GradeBadge)에서 결정.
 */
import { type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type BadgeTone = "good" | "fair" | "warn" | "bad" | "neutral";

interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
  className?: string;
}

const TONE_CLASSES: Record<BadgeTone, string> = {
  good: "bg-green-100 text-green-800 border-green-200",
  fair: "bg-lime-100 text-lime-800 border-lime-200",
  warn: "bg-amber-100 text-amber-800 border-amber-200",
  bad: "bg-red-100 text-red-800 border-red-200",
  neutral: "bg-zinc-100 text-zinc-700 border-zinc-200",
};

export function Badge({ tone = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium",
        TONE_CLASSES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
