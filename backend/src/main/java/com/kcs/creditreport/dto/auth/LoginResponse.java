package com.kcs.creditreport.dto.auth;

/**
 * 로그인 성공 응답 바디 (API 명세서 §3.2) — 200 OK.
 *
 * <p>Access Token 은 응답 바디로 내려 클라이언트 메모리에 보관시킨다.
 * Refresh Token 은 이 바디에 포함하지 않고 HttpOnly 쿠키로만 전달한다(JS 접근 차단).
 *
 * @param accessToken Access Token(JWT)
 * @param tokenType   고정값 {@code Bearer}
 * @param expiresIn   Access 만료(초). 예: 900(15분)
 * @param user        로그인 사용자 요약
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    /**
     * 로그인 사용자 요약.
     *
     * @param id    회원 ID
     * @param email 이메일
     */
    public record UserSummary(Long id, String email) {
    }

    public static LoginResponse of(String accessToken, long expiresInSeconds, Long userId, String email) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresInSeconds,
                new UserSummary(userId, email)
        );
    }
}
