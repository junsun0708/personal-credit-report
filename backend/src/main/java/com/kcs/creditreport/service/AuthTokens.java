package com.kcs.creditreport.service;

/**
 * 서비스 → 컨트롤러로 전달하는 토큰 묶음(계층 경계 DTO).
 *
 * <p>서비스는 HTTP/쿠키를 직접 다루지 않으므로(설계서 §2.2), 생성한 Access·Refresh 원문과
 * 사용자 식별 정보만 반환하고, 쿠키화/응답 매핑은 컨트롤러가 수행한다.
 *
 * @param accessToken  Access Token(JWT) — 응답 바디로 전달 예정
 * @param refreshToken Refresh Token(JWT) — 쿠키로 전달 예정
 * @param userId       회원 ID
 * @param email        이메일
 */
public record AuthTokens(
        String accessToken,
        String refreshToken,
        Long userId,
        String email
) {
}
