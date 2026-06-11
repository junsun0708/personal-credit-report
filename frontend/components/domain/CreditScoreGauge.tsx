/**
 * 신용점수 게이지(도메인 결합).
 * ui/Gauge를 한 방향 합성한다. value/max를 받아 재사용 가능(개발표준 §5.3).
 * 등급 톤에 따라 게이지 색을 매핑한다.
 */
import { Gauge } from "@/components/ui/Gauge";
import { getGradeTone, type GradeTone } from "@/lib/utils/grade";

interface CreditScoreGaugeProps {
  value: number;
  /** 만점. 기본 1000 */
  max?: number;
  /** 색 매핑에 사용할 등급(있으면 톤별 색, 없으면 기본 파랑) */
  grade?: number;
  label?: string;
}

const TONE_COLOR: Record<GradeTone, string> = {
  good: "text-green-600",
  fair: "text-lime-600",
  warn: "text-amber-500",
  bad: "text-red-600",
};

export function CreditScoreGauge({
  value,
  max = 1000,
  grade,
  label = "신용점수",
}: CreditScoreGaugeProps) {
  const colorClassName =
    grade != null ? TONE_COLOR[getGradeTone(grade)] : "text-blue-600";
  return <Gauge value={value} max={max} label={label} colorClassName={colorClassName} />;
}
