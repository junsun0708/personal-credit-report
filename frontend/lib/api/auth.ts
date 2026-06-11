/**
 * 인증 API 호출 모듈.
 * 백엔드 /api/auth/* 엔드포인트를 래핑한다.
 */
import { apiClient } from "@/lib/api/client";
import type {
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  RefreshResponse,
  SignupRequest,
  SignupResponse,
} from "@/types/auth";

/** 회원가입 — 201 {id,email,createdAt} / 400 VALIDATION_ERROR / 409 EMAIL_DUPLICATED */
export async function signup(body: SignupRequest): Promise<SignupResponse> {
  const res = await apiClient.post<SignupResponse>("/api/auth/signup", body);
  return res.data;
}

/** 로그인 — 200 {accessToken,...,user} + Set-Cookie(refresh) / 401 INVALID_CREDENTIALS */
export async function login(body: LoginRequest): Promise<LoginResponse> {
  const res = await apiClient.post<LoginResponse>("/api/auth/login", body);
  return res.data;
}

/**
 * silent refresh — 쿠키만으로 새 Access 발급. 앱 마운트 시 세션 복원에 사용.
 * (인터셉터의 자동 재발급과 별개로, 명시적 호출이 필요한 부트스트랩 경로)
 */
export async function refresh(): Promise<RefreshResponse> {
  const res = await apiClient.post<RefreshResponse>("/api/auth/refresh");
  return res.data;
}

/** 로그아웃 — Bearer 필요. 서버 측 refresh 쿠키 무효화. */
export async function logout(): Promise<LogoutResponse> {
  const res = await apiClient.post<LogoutResponse>("/api/auth/logout");
  return res.data;
}
