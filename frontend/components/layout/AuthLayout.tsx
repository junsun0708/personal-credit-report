"use client";

/**
 * 인증(비로그인) 그룹 레이아웃.
 *
 * - 이미 인증된 사용자가 접근하면 /reports로 보낸다.
 * - 중앙 정렬 카드, 헤더 없음(설계서 §1.1).
 * - 부트스트랩 미완 시 잠깐 로딩(인증 사용자 깜빡임 방지).
 */
import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/authStore";
import { Spinner } from "@/components/ui/Spinner";

interface AuthLayoutProps {
  children: ReactNode;
}

export function AuthLayout({ children }: AuthLayoutProps) {
  const router = useRouter();
  const accessToken = useAuthStore((s) => s.accessToken);
  const isAuthResolved = useAuthStore((s) => s.isAuthResolved);
  const isAuthenticated = Boolean(accessToken);

  useEffect(() => {
    if (isAuthResolved && isAuthenticated) {
      router.replace("/reports");
    }
  }, [isAuthResolved, isAuthenticated, router]);

  // 인증된 사용자라면 리다이렉트 대기 중 로딩만
  if (isAuthResolved && isAuthenticated) {
    return (
      <div className="flex min-h-full items-center justify-center bg-zinc-50">
        <Spinner size={28} className="text-zinc-400" />
      </div>
    );
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-zinc-50 px-4 py-12">
      <div className="w-full max-w-md">{children}</div>
    </div>
  );
}
