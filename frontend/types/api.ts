/**
 * API 공통 타입.
 * 백엔드 응답 래퍼·에러 포맷·페이지 응답을 정의한다.
 */

/** 백엔드 공통 에러 바디: { code, message } */
export interface ApiError {
  code: string;
  message: string;
}

/**
 * 서버 페이징 공통 응답 래퍼.
 * page는 1-base, size 기본 10.
 */
export interface PageResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 알려진 에러 코드(문서화·분기용). 그 외 코드도 문자열로 허용. */
export type KnownErrorCode =
  | "VALIDATION_ERROR"
  | "EMAIL_DUPLICATED"
  | "INVALID_CREDENTIALS"
  | "INVALID_REFRESH_TOKEN"
  | "NOT_FOUND";
