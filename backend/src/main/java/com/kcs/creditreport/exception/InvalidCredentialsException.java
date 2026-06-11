package com.kcs.creditreport.exception;

/**
 * 로그인 시 이메일/비밀번호 불일치. → 401 INVALID_CREDENTIALS.
 *
 * <p>보안: 이메일 미존재와 비밀번호 불일치를 동일 예외로 처리해 사용자 열거를 방지한다.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
