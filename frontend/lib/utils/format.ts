/**
 * 표시용 포맷터 유틸.
 * 도메인 무지의 순수 함수만 둔다.
 */

/** 숫자에 천단위 콤마. (예: 1234567 → "1,234,567") */
export function formatNumber(value: number): string {
  return value.toLocaleString("ko-KR");
}

/** 금액(원) 표시. (예: 1234567 → "1,234,567원") */
export function formatCurrency(value: number): string {
  return `${formatNumber(value)}원`;
}

/**
 * ISO datetime 또는 yyyy-MM-dd 문자열을 "yyyy-MM-dd HH:mm:ss" 로 표시.
 * 파싱 불가 시 원본을 반환한다(방어적 처리).
 */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  const pad = (n: number): string => String(n).padStart(2, "0");
  const y = date.getFullYear();
  const mo = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  const h = pad(date.getHours());
  const mi = pad(date.getMinutes());
  const s = pad(date.getSeconds());
  return `${y}-${mo}-${d} ${h}:${mi}:${s}`;
}

/** yyyy-MM-dd 날짜 문자열을 그대로 표시(이미 표준 포맷). 빈 값은 "-". */
export function formatDate(value: string | null | undefined): string {
  return value && value.length > 0 ? value : "-";
}
