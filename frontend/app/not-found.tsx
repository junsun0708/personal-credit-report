import Link from "next/link";

/**
 * 404 / 잘못된 리포트 ID 폴백(SCR-404).
 */
export default function NotFound() {
  return (
    <div className="flex min-h-full flex-col items-center justify-center gap-4 bg-zinc-50 px-4 text-center">
      <p className="text-5xl font-bold text-zinc-300">404</p>
      <h1 className="text-lg font-semibold text-zinc-800">
        요청하신 페이지를 찾을 수 없습니다
      </h1>
      <p className="text-sm text-zinc-500">
        존재하지 않거나 접근 권한이 없는 리포트일 수 있습니다.
      </p>
      <Link
        href="/reports"
        className="mt-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        리포트 목록으로
      </Link>
    </div>
  );
}
