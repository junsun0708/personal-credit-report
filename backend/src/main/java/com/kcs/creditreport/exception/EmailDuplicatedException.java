package com.kcs.creditreport.exception;

/**
 * 이미 가입된 이메일로 회원가입을 시도할 때 발생. → 409 EMAIL_DUPLICATED.
 */
public class EmailDuplicatedException extends BusinessException {

    public EmailDuplicatedException() {
        super(ErrorCode.EMAIL_DUPLICATED);
    }
}
