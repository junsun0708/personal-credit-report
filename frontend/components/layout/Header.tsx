"use client";

/**
 * 앱 헤더(레이아웃).
 * 로고 + 네비(리포트목록/조회이력) + 계정 메뉴(이메일·로그아웃).
 */
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuthStore } from "@/stores/authStore";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils/cn";

const NAV_ITEMS = [
  { href: "/reports", label: "리포트목록" },
  { href: "/history", label: "조회이력" },
];

export function Header() {
  const pathname = usePathname();
  const user = useAuthStore((s) => s.user);
  const { logoutMutation } = useAuth();

  return (
    <header className="sticky top-0 z-40 border-b border-zinc-200 bg-white">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-4 px-4">
        <div className="flex items-center gap-6">
          <Link href="/reports" className="flex items-center gap-2 font-bold text-zinc-900">
            <span aria-hidden="true">📊</span>
            신용 리포트
          </Link>
          <nav className="flex items-center gap-1">
            {NAV_ITEMS.map((item) => {
              const active = pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                    active
                      ? "bg-blue-50 text-blue-700"
                      : "text-zinc-600 hover:bg-zinc-100",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {user && (
            <span className="hidden text-sm text-zinc-500 sm:inline">{user.email}</span>
          )}
          <Button
            variant="ghost"
            size="sm"
            loading={logoutMutation.isPending}
            onClick={() => logoutMutation.mutate()}
          >
            로그아웃
          </Button>
        </div>
      </div>
    </header>
  );
}
