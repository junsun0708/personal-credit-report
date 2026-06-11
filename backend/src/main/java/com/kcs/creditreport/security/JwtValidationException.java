package com.kcs.creditreport.security;

import com.kcs.creditreport.exception.BusinessException;
import com.kcs.creditreport.exception.ErrorCode;

/**
 * JWT 검증 실패 예외(만료·위조·종류불일치·형식오류).
 *
 * <p>{@link BusinessException} 을 상속해 보유 {@link ErrorCode}(TOKEN_EXPIRED /
 * UNAUTHORIZED / INVALID_REFRESH_TOKEN)에 따라 정확한 401 응답으로 매핑된다.
 *
 * <p>필터 단계에서 발생하면 SecurityContext 를 비운 채 통과시키고, 인가 실패 시
 * EntryPoint 가 응답을 생성한다. 서비스 단계(refresh)에서 발생하면 전역 핸들러가 처리한다.
 */
public class JwtValidationException extends BusinessException {

    public JwtValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
