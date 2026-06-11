package com.kcs.creditreport.dto.auth;

/**
 * Access 재발급 응답 바디 (API 명세서 §3.3) — 200 OK.
 *
 * <p>로그인 응답과 달리 {@code user} 를 포함하지 않는다(이미 인증된 세션의 토큰 갱신).
 * Refresh 회전 시 새 Refresh 쿠키는 헤더(Set-Cookie)로 별도 전달된다.
 *
 * @param accessToken 새 Access Token(JWT)
 * @param tokenType   고정값 {@code Bearer}
 * @param expiresIn   Access 만료(초)
 */
public record TokenRefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, long expiresInSeconds) {
        return new TokenRefreshResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
