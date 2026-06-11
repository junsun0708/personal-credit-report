package com.kcs.creditreport.security;

/**
 * 토큰 종류. JWT 클레임({@code typ})에 담아 Access/Refresh 를 구분한다.
 *
 * <p>보안 의도: Refresh 토큰을 Access 자리(Authorization 헤더)에 재사용하거나 그 반대를
 * 막기 위해 발급 시 종류를 명시하고, 검증 시 기대 종류와 일치하는지 확인한다(토큰 혼용 방어).
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
