package com.kcs.creditreport.exception;

/**
 * Refresh 쿠키 누락·만료·위변조·폐기(로그아웃됨). → 401 INVALID_REFRESH_TOKEN.
 */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
