"use client";

/**
 * 인증 액션 훅.
 * 로그인·회원가입·로그아웃 mutation과 store 연동을 한 곳에 모은다.
 */
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { login as loginApi, logout as logoutApi, signup as signupApi } from "@/lib/api/auth";
import { useAuthStore } from "@/stores/authStore";
import type { LoginRequest, SignupRequest } from "@/types/auth";

export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((s) => s.setAuth);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  const loginMutation = useMutation({
    mutationFn: (body: LoginRequest) => loginApi(body),
    onSuccess: (data) => {
      // Access는 메모리(Zustand), Refresh는 Set-Cookie로 자동 보관
      setAuth(data.accessToken, data.user);
    },
  });

  const signupMutation = useMutation({
    mutationFn: (body: SignupRequest) => signupApi(body),
  });

  const logoutMutation = useMutation({
    mutationFn: () => logoutApi(),
    // 성공·실패와 무관하게 클라이언트 세션은 정리(서버 오류여도 로그아웃 보장)
    onSettled: () => {
      clearAuth();
      queryClient.clear();
      router.replace("/login");
    },
  });

  return { loginMutation, signupMutation, logoutMutation };
}
