"use client";

/**
 * TanStack Query Provider.
 * 앱 루트에서 단일 QueryClient를 주입한다.
 *
 * - QueryClient는 useState로 1회 생성하여 리렌더 시 재생성을 막는다(SSR/HMR 안전).
 * - 기본 staleTime/gcTime을 설정. 401 등 인증 에러는 Axios 인터셉터가 재발급으로
 *   처리하므로 Query의 retry는 보수적으로 1회만 둔다(중복 시도·깜빡임 방지).
 */
import { useState, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

interface QueryProviderProps {
  children: ReactNode;
}

export function QueryProvider({ children }: QueryProviderProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000, // 30초: 목록·상세 기본 신선도
            gcTime: 5 * 60_000, // 5분: 캐시 보관
            retry: 1,
            refetchOnWindowFocus: false,
          },
          mutations: {
            retry: 0,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
