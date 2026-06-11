import { type ReactNode } from "react";
import { ProtectedLayout } from "@/components/layout/ProtectedLayout";

/**
 * (protected) 그룹 레이아웃 — 인증 필수 영역.
 * 미인증 시 /login 리다이렉트 + AppShell(헤더) 래핑(ProtectedLayout 내부 처리).
 */
export default function ProtectedGroupLayout({ children }: { children: ReactNode }) {
  return <ProtectedLayout>{children}</ProtectedLayout>;
}
