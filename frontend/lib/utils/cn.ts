/**
 * 클래스네임 조합 헬퍼.
 * 외부 의존(clsx/tailwind-merge) 없이 falsy 값을 제거하고 공백으로 합친다.
 */
export type ClassValue = string | number | false | null | undefined;

export function cn(...values: ClassValue[]): string {
  return values.filter((v): v is string | number => Boolean(v)).join(" ");
}
