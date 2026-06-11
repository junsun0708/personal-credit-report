/**
 * 반원형 게이지(도메인 무지).
 *
 * - value/max/label만 받아 어떤 점수 시각화에도 재사용(개발표준 §5.3).
 * - SVG arc로 반원 게이지를 그리고 중앙에 값/비율을 표시한다.
 */
import { cn } from "@/lib/utils/cn";

interface GaugeProps {
  value: number;
  max: number;
  label?: string;
  /** 진행 색(Tailwind text-* 클래스의 currentColor 기반). 기본 파랑 */
  colorClassName?: string;
  className?: string;
}

const SIZE = 200;
const STROKE = 18;
const RADIUS = (SIZE - STROKE) / 2;
// 반원 둘레 길이
const SEMI_CIRCUMFERENCE = Math.PI * RADIUS;

export function Gauge({
  value,
  max,
  label,
  colorClassName = "text-blue-600",
  className,
}: GaugeProps) {
  const safeMax = max > 0 ? max : 1;
  const ratio = Math.min(Math.max(value / safeMax, 0), 1);
  const percent = Math.round(ratio * 100);
  // 반원 경로: 좌하단 → 우하단 (위로 볼록)
  const cy = SIZE / 2;
  const arcPath = `M ${STROKE / 2} ${cy} A ${RADIUS} ${RADIUS} 0 0 1 ${SIZE - STROKE / 2} ${cy}`;
  const dashOffset = SEMI_CIRCUMFERENCE * (1 - ratio);

  return (
    <div className={cn("flex flex-col items-center", className)}>
      {label && <span className="mb-1 text-sm font-semibold text-zinc-500">{label}</span>}
      <div className="relative" style={{ width: SIZE, height: SIZE / 2 + STROKE }}>
        <svg
          width={SIZE}
          height={SIZE / 2 + STROKE}
          viewBox={`0 0 ${SIZE} ${SIZE / 2 + STROKE}`}
          role="img"
          aria-label={`${label ?? "점수"} ${value} / ${max} (${percent}%)`}
        >
          {/* 트랙 */}
          <path
            d={arcPath}
            fill="none"
            stroke="currentColor"
            className="text-zinc-200"
            strokeWidth={STROKE}
            strokeLinecap="round"
          />
          {/* 진행 */}
          <path
            d={arcPath}
            fill="none"
            stroke="currentColor"
            className={colorClassName}
            strokeWidth={STROKE}
            strokeLinecap="round"
            strokeDasharray={SEMI_CIRCUMFERENCE}
            strokeDashoffset={dashOffset}
          />
        </svg>
        <div className="absolute inset-x-0 bottom-0 flex flex-col items-center">
          <span className="text-3xl font-bold text-zinc-900">{value}</span>
          <span className="text-xs text-zinc-500">/ {max}</span>
        </div>
      </div>
      <span className="mt-1 text-sm font-medium text-zinc-600">{percent}%</span>
    </div>
  );
}
