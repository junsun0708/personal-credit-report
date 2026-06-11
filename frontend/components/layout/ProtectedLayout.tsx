"use client";

/**
 * 보호 라우트 가드 + AppShell 래핑(레이아웃).
 *
 * - silent refresh가 끝나기 전(isAuthResolved=false)에는 로딩만 표시(깜빡임 방지).
 * - 해소 후 미인증이면 returnUrl을 보존하며 /login으로 리다이렉트.
 * - 인증 상태면 AppShell(헤더 포함)로 children을 감싼다.
 */
import { Suspense, useEffect, type ReactNode } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/stores/authStore";
import { AppShell } from "@/components/layout/AppShell";
import { Spinner } from "@/components/ui/Spinner";

interface ProtectedLayoutProps {
  children: ReactNode;
}

/** 로딩 중앙 스피너(공통) */
function GuardFallback() {
  return (
    <div className="flex min-h-full items-center justify-center bg-zinc-50">
      <Spinner size={28} className="text-zinc-400" />
    </div>
  );
}

/** useSearchParams를 사용하는 가드 본체(Suspense 경계 내부에서만 렌더) */
function ProtectedGuard({ children }: ProtectedLayoutProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const accessToken = useAuthStore((s) => s.accessToken);
  const isAuthResolved = useAuthStore((s) => s.isAuthResolved);

  const isAuthenticated = Boolean(accessToken);

  useEffect(() => {
    if (isAuthResolved && !isAuthenticated) {
      // 원경로(+쿼리) 보존하여 로그인 후 복귀
      const qs = searchParams.toString();
      const returnUrl = qs ? `${pathname}?${qs}` : pathname;
      router.replace(`/login?returnUrl=${encodeURIComponent(returnUrl)}`);
    }
  }, [isAuthResolved, isAuthenticated, pathname, searchParams, router]);

  // 부트스트랩 미완 또는 미인증(리다이렉트 대기) 시 로딩 표시
  if (!isAuthResolved || !isAuthenticated) {
    return <GuardFallback />;
  }

  return <AppShell>{children}</AppShell>;
}

export function ProtectedLayout({ children }: ProtectedLayoutProps) {
  // useSearchParams 사용을 Suspense로 감싸 CSR bailout 경고 방지
  return (
    <Suspense fallback={<GuardFallback />}>
      <ProtectedGuard>{children}</ProtectedGuard>
    </Suspense>
  );
}
