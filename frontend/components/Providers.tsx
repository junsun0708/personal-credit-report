"use client";

/**
 * 앱 전역 Provider 묶음(클라이언트 경계).
 * 루트 레이아웃(서버 컴포넌트)에서 children을 감싸 주입한다.
 *
 * 순서: QueryProvider → ToastProvider → AuthBootstrap.
 * (AuthBootstrap은 Query·Toast 컨텍스트와 무관하지만, 일관된 단일 경계로 묶는다.)
 */
import { type ReactNode } from "react";
import { QueryProvider } from "@/lib/query/QueryProvider";
import { ToastProvider } from "@/components/ui/Toast";
import { AuthBootstrap } from "@/components/domain/AuthBootstrap";

interface ProvidersProps {
  children: ReactNode;
}

export function Providers({ children }: ProvidersProps) {
  return (
    <QueryProvider>
      <ToastProvider>
        <AuthBootstrap>{children}</AuthBootstrap>
      </ToastProvider>
    </QueryProvider>
  );
}
