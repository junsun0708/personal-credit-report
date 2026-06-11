/**
 * 앱 공통 골격(레이아웃).
 * 헤더 + 중앙 컨테이너 본문.
 */
import { type ReactNode } from "react";
import { Header } from "@/components/layout/Header";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="flex min-h-full flex-col bg-zinc-50">
      <Header />
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">{children}</main>
    </div>
  );
}
