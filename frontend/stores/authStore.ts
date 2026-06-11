/**
 * 인증 클라이언트 상태(Zustand).
 *
 * - accessToken·user를 메모리에만 보관(영속 X). 새로고침 시 silent refresh로 복원.
 * - refresh 토큰은 HttpOnly 쿠키에 있어 JS가 접근하지 않는다.
 * - 초기 silent refresh 완료 여부(isAuthResolved)로 라우트 가드의 깜빡임을 방지한다.
 */
import { create } from "zustand";
import type { User } from "@/types/auth";

interface AuthState {
  accessToken: string | null;
  user: User | null;
  /** 앱 마운트 시 silent refresh 시도가 끝났는지 여부 */
  isAuthResolved: boolean;
  /** 로그인/재발급 성공 시 세션 설정 */
  setAuth: (accessToken: string, user: User) => void;
  /** 토큰만 갱신(refresh 성공 시 user는 유지) */
  setAccessToken: (accessToken: string) => void;
  /** 로그아웃/세션 만료 시 정리 */
  clearAuth: () => void;
  /** silent refresh 완료 표시 */
  markResolved: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthResolved: false,
  setAuth: (accessToken, user) => set({ accessToken, user }),
  setAccessToken: (accessToken) => set({ accessToken }),
  clearAuth: () => set({ accessToken: null, user: null }),
  markResolved: () => set({ isAuthResolved: true }),
}));

/**
 * React 외부(Axios 인터셉터 등)에서 현재 토큰을 읽고 쓰기 위한 헬퍼.
 * 컴포넌트 리렌더와 무관하게 store 스냅샷에 직접 접근한다.
 */
export const authStore = {
  getAccessToken: (): string | null => useAuthStore.getState().accessToken,
  setAccessToken: (token: string): void => useAuthStore.getState().setAccessToken(token),
  clear: (): void => useAuthStore.getState().clearAuth(),
};
