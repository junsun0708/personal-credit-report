package com.kcs.creditreport.dto;

import com.kcs.creditreport.exception.ErrorCode;

/**
 * 공통 에러 응답 바디 (API 명세서 §1.5).
 *
 * <pre>{@code { "code": "INVALID_CREDENTIALS", "message": "..." } }</pre>
 *
 * @param code    기계 판독용 에러 코드(UPPER_SNAKE_CASE)
 * @param message 사용자/개발자용 한국어 설명
 */
public record ErrorResponse(String code, String message) {

    /** ErrorCode 의 기본 메시지로 생성. */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage());
    }

    /** 메시지를 오버라이드해 생성(예: 필드 검증 상세 메시지). */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
