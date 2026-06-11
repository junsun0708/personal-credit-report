"use client";

/**
 * 앱 마운트 시 인증 부트스트랩(클라이언트 전용).
 *
 * 역할:
 * 1) silent refresh — refresh 쿠키로 Access를 1회 시도해 새로고침 후에도 세션 유지.
 *    (이 단계에서 user는 응답에 없으므로, 토큰만 복원하고 user는 후속 보호 페이지가
 *     필요 시 채운다. 여기선 토큰 존재 = 인증으로 간주.)
 * 2) refresh 실패 핸들러 등록 — 인터셉터의 자동 재발급이 최종 실패하면 세션 정리 후 /login.
 * 3) 완료 시 markResolved()로 라우트 가드의 초기 깜빡임을 방지.
 *
 * 화면을 그리지 않는다(children 통과).
 */
import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { refresh as refreshApi } from "@/lib/api/auth";
import { setOnRefreshFailure } from "@/lib/api/client";
import { useAuthStore } from "@/stores/authStore";

interface AuthBootstrapProps {
  children: ReactNode;
}

export function AuthBootstrap({ children }: AuthBootstrapProps) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const markResolved = useAuthStore((s) => s.markResolved);
  const isAuthResolved = useAuthStore((s) => s.isAuthResolved);

  useEffect(() => {
    // 인터셉터 refresh 최종 실패 시 동작 주입
    setOnRefreshFailure(() => {
      clearAuth();
      router.replace("/login");
    });
  }, [clearAuth, router]);

  useEffect(() => {
    let active = true;
    // 이미 해소되었으면(HMR 등) 재시도하지 않음
    if (isAuthResolved) return;

    void (async () => {
      try {
        const data = await refreshApi();
        if (active) setAccessToken(data.accessToken);
      } catch {
        // refresh 쿠키 없음/만료 → 미인증 상태로 진행(보호 라우트가 /login 처리)
        if (active) clearAuth();
      } finally {
        if (active) markResolved();
      }
    })();

    return () => {
      active = false;
    };
    // 최초 1회만 실행
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <>{children}</>;
}
