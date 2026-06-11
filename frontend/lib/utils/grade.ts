/**
 * 신용등급(1~10) 표시 보조 유틸.
 * 색상·라벨 매핑을 한 곳에 모아 GradeBadge 등에서 재사용한다.
 */

/** 등급 구간별 색상 톤 키(Tailwind 클래스는 컴포넌트에서 매핑) */
export type GradeTone = "good" | "fair" | "warn" | "bad";

/**
 * 등급(1~10)을 4단계 톤으로 분류한다.
 * 1~2 우수(good), 3~4 양호(fair), 5~7 주의(warn), 8~10 위험(bad).
 */
export function getGradeTone(grade: number): GradeTone {
  if (grade <= 2) return "good";
  if (grade <= 4) return "fair";
  if (grade <= 7) return "warn";
  return "bad";
}

/** 등급 톤별 한국어 라벨 */
export function getGradeLabel(grade: number): string {
  switch (getGradeTone(grade)) {
    case "good":
      return "우수";
    case "fair":
      return "양호";
    case "warn":
      return "주의";
    case "bad":
      return "위험";
  }
}

/** 등급 톤별 신호등 이모지(색 외 보조 식별자) */
export function getGradeEmoji(grade: number): string {
  switch (getGradeTone(grade)) {
    case "good":
      return "🟢";
    case "fair":
      return "🟡";
    case "warn":
      return "🟠";
    case "bad":
      return "🔴";
  }
}
