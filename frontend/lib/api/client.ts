/**
 * Axios 인스턴스 + 401 자동 재발급 인터셉터.
 *
 * 핵심 동작:
 * 1) 요청 인터셉터 — 메모리의 Access Token을 Authorization 헤더에 주입.
 * 2) 응답 인터셉터 — 401 수신 시 /api/auth/refresh로 새 Access를 받아 원요청을 1회 재시도.
 * 3) 동시 401 큐잉 — 여러 요청이 동시에 만료를 만나도 refresh는 단 1회만 수행하고,
 *    진행 중이면 대기 큐에 적재했다가 새 토큰으로 일괄 재개(중복 refresh 방지).
 * 4) refresh 실패 시 — 세션 정리 후 onRefreshFailure 콜백으로 /login 이동을 위임.
 *
 * refresh 토큰은 HttpOnly 쿠키에 있으므로 withCredentials: true 로 자동 전송된다.
 */
import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { authStore } from "@/stores/authStore";
import type { RefreshResponse } from "@/types/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** 재시도 여부를 표시하는 확장 요청 설정(무한 루프 방지용 플래그) */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/**
 * 메인 API 클라이언트.
 * 모든 도메인 요청(auth/reports/histories)이 이 인스턴스를 공유한다.
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/**
 * refresh 전용 클라이언트(인터셉터 없는 깨끗한 인스턴스).
 * refresh 요청 자체가 401을 받아 또 인터셉터를 타는 재귀를 차단한다.
 */
const refreshClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/** refresh 실패(세션 만료) 시 호출되는 콜백. 앱 루트에서 /login 이동을 주입한다. */
let onRefreshFailure: (() => void) | null = null;

export function setOnRefreshFailure(handler: () => void): void {
  onRefreshFailure = handler;
}

// ── 동시 401 큐잉 상태 ────────────────────────────────────────────────
/** 현재 진행 중인 refresh Promise(있으면 새 요청은 여기에 합류) */
let refreshPromise: Promise<string> | null = null;

/**
 * /api/auth/refresh 를 호출해 새 Access Token을 받아 store에 저장한다.
 * 동시 호출이 들어와도 단일 Promise를 공유(single-flight)한다.
 */
async function performRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<RefreshResponse>("/api/auth/refresh")
      .then((res) => {
        const { accessToken } = res.data;
        authStore.setAccessToken(accessToken);
        return accessToken;
      })
      .finally(() => {
        // 성공·실패와 무관하게 다음 라운드를 위해 초기화
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

// ── 요청 인터셉터: Access Token 주입 ─────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// ── 응답 인터셉터: 401 자동 재발급 ───────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableConfig | undefined;

    // 네트워크 오류 등 config가 없거나 401이 아니면 그대로 전파
    if (!originalRequest || error.response?.status !== 401) {
      return Promise.reject(error);
    }

    // refresh 요청 자체의 401 또는 이미 1회 재시도한 요청은 더 시도하지 않음
    const url = originalRequest.url ?? "";
    if (originalRequest._retry || url.includes("/api/auth/refresh")) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      // 진행 중 refresh가 있으면 합류, 없으면 새로 시작(중복 방지)
      const newToken = await performRefresh();
      originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
      return apiClient(originalRequest);
    } catch (refreshError) {
      // refresh 실패 → 세션 정리 + 로그인 화면으로 위임
      authStore.clear();
      onRefreshFailure?.();
      return Promise.reject(refreshError);
    }
  },
);

/**
 * 응답 에러에서 백엔드 공통 에러 코드를 안전하게 추출한다.
 * 타입 안정성을 위해 unknown을 좁혀 사용한다.
 */
export function extractErrorCode(error: unknown): string | null {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object" && "code" in data) {
      const code = (data as { code: unknown }).code;
      return typeof code === "string" ? code : null;
    }
  }
  return null;
}

/** 응답 에러에서 사용자 노출용 메시지를 추출한다(없으면 fallback). */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object" && "message" in data) {
      const message = (data as { message: unknown }).message;
      if (typeof message === "string" && message.length > 0) {
        return message;
      }
    }
  }
  return fallback;
}

export type { AxiosRequestConfig };
