import { type ReactNode } from "react";
import { AuthLayout } from "@/components/layout/AuthLayout";

/**
 * (auth) 그룹 레이아웃 — 로그인·회원가입.
 * 인증된 사용자는 /reports로 보낸다(AuthLayout 내부 처리).
 */
export default function AuthGroupLayout({ children }: { children: ReactNode }) {
  return <AuthLayout>{children}</AuthLayout>;
}
