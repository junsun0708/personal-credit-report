package com.kcs.creditreport.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 최상위 런타임 예외.
 *
 * <p>{@link ErrorCode} 를 보유하며, 전역 예외 처리기({@code GlobalExceptionHandler})가 이를
 * 받아 표준 {@code {code, message}} 응답과 적절한 HTTP 상태로 변환한다.
 *
 * <p>서비스 계층은 도메인 규칙 위반 시 이 예외(또는 하위 타입)를 던지고, HTTP 관심사를
 * 직접 다루지 않는다(계층 책임 분리 — 설계서 §2.2).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 코드의 기본 메시지를 사용. */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** 메시지를 커스터마이즈(단, 민감정보·내부 상세 노출 금지). */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
