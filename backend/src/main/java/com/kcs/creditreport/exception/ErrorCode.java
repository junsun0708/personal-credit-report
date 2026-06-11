package com.kcs.creditreport.exception;

import org.springframework.http.HttpStatus;

/**
 * 표준 에러 코드 (API 명세서 §1.5 공통 에러 코드 목록).
 *
 * <p>각 코드는 HTTP 상태와 기본 한국어 메시지를 함께 보유한다. 전역 예외 처리기는 이 enum 을
 * 단일 진실 공급원(SSOT)으로 사용해 {@code {code, message}} 응답을 생성한다.
 *
 * <p>보안 메모: {@code INVALID_CREDENTIALS} 메시지는 "이메일 또는 비밀번호"로 일반화하여
 * 사용자 열거(account enumeration) 공격을 방지한다(이메일 존재 여부를 응답으로 구분 불가).
 */
public enum ErrorCode {

    /** 요청 값 검증 실패(형식·정책 위반). */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    /** 인증 정보 불일치(로그인 실패). 열거 방지 위해 메시지 일반화. */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    /** Access Token 누락·만료·위변조. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    /** Access Token 만료 — 클라이언트 refresh 트리거 용도. */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),

    /** Refresh Token 누락·만료·폐기됨. */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

    /** 인증은 되었으나 권한 없음(타인 리소스 접근 등). */
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /** 리소스 없음. */
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    /** 이미 가입된 이메일. */
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),

    /** 서버 내부 오류 — 상세는 비노출, 로깅만. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
