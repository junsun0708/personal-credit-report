/**
 * 신용등급 배지(도메인 결합).
 * 등급(1~10)을 받아 ui/Badge를 톤·라벨과 함께 합성한다(목록·상세 공용).
 */
import { Badge } from "@/components/ui/Badge";
import { getGradeEmoji, getGradeLabel, getGradeTone } from "@/lib/utils/grade";

interface GradeBadgeProps {
  grade: number;
  /** 등급 라벨(우수/양호 등) 함께 표시 여부. 기본 false(목록은 간결하게) */
  withLabel?: boolean;
  className?: string;
}

export function GradeBadge({ grade, withLabel = false, className }: GradeBadgeProps) {
  return (
    <Badge tone={getGradeTone(grade)} className={className}>
      <span aria-hidden="true">{getGradeEmoji(grade)}</span>
      {grade}등급{withLabel ? ` · ${getGradeLabel(grade)}` : ""}
    </Badge>
  );
}
