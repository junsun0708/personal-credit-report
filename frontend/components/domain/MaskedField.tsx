/**
 * 마스킹 값 표시(도메인 결합).
 * 서버에서 이미 마스킹된 값을 그대로 렌더한다(클라이언트 마스킹 아님).
 * label-value 행 형태로 기본 정보 카드에서 사용.
 */
interface MaskedFieldProps {
  label: string;
  maskedValue: string;
}

export function MaskedField({ label, maskedValue }: MaskedFieldProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-2">
      <span className="text-sm text-zinc-500">{label}</span>
      <span className="font-mono text-sm tracking-wider text-zinc-900">
        {maskedValue}
      </span>
    </div>
  );
}
