/**
 * 인증 도메인 타입.
 * 백엔드 /api/auth/* 계약과 1:1 대응한다.
 */

/** 로그인 응답·세션에서 사용하는 사용자 정보 */
export interface User {
  id: number;
  email: string;
}

/** 회원가입 응답 바디 */
export interface SignupResponse {
  id: number;
  email: string;
  createdAt: string;
}

/** 로그인 요청 바디 */
export interface LoginRequest {
  email: string;
  password: string;
}

/** 회원가입 요청 바디 */
export interface SignupRequest {
  email: string;
  password: string;
}

/** 로그인 응답 바디 (Set-Cookie로 refresh 별도 발급) */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

/** refresh 응답 바디 (쿠키 기반, 새 Access만 반환) */
export interface RefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

/** logout 응답 바디 */
export interface LogoutResponse {
  message: string;
}
