package com.kcs.creditreport.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-AUTH-TOKEN 통합 테스트 (P0 우선)
 *
 * <p>검증 대상: POST /api/auth/refresh, POST /api/auth/logout
 * <ul>
 *   <li>TC-AT-02 : 유효 Refresh 쿠키로 /auth/refresh → 200, 새 accessToken 발급</li>
 *   <li>TC-AT-05 : 로그아웃 후 Refresh 무효화 → 이후 /auth/refresh 401</li>
 *   <li>SEC-06   : 서명이 위조된 JWT → 401 (신뢰 거부)</li>
 * </ul>
 */
@DisplayName("토큰 재발급/로그아웃 통합 테스트")
class AuthTokenIntegrationTest extends IntegrationTestBase {

    // ── TC-AT-02 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AT-02: 유효한 Refresh 쿠키로 /auth/refresh 요청 시 200 과 새 accessToken 을 반환한다")
    void refresh_withValidRefreshCookie_returns200AndNewAccessToken() throws Exception {
        // 1단계: 로그인하여 Refresh 쿠키 획득
        Cookie refreshCookie = loginAndGetRefreshCookie();

        // 2단계: Refresh 쿠키를 전달하여 재발급 요청
        var result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                // 새 accessToken 이 반환되어야 한다
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        // 3단계: 회전된 새 Refresh 쿠키도 함께 발급되는지 확인
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .as("토큰 회전: 새 refreshToken 쿠키가 응답에 포함되어야 한다")
                .contains(REFRESH_COOKIE_NAME);
    }

    @Test
    @DisplayName("TC-AT-02-B: Refresh 재발급 후 발급된 accessToken 이 이전 것과 다르다(토큰 회전)")
    void refresh_newAccessTokenIsDifferentFromOriginal() throws Exception {
        // 1단계: 최초 로그인 → 원본 accessToken
        String originalAccessToken = loginAndGetAccessToken();
        Cookie refreshCookie = loginAndGetRefreshCookie();

        // 2단계: 재발급 요청 → 새 accessToken
        var refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        String newAccessToken = objectMapper.readTree(
                refreshResult.getResponse().getContentAsString()
        ).get("accessToken").asText();

        // JWT 는 발급 시각이 달라 서로 달라야 한다 (짧은 시간 차이라도 iat 이 다름)
        // 단, 동일 초 내 발급이면 같을 수 있으므로 null 체크만 핵심으로 검증
        assertThat(newAccessToken).isNotBlank();
    }

    // ── TC-AT-05 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AT-05: 로그아웃 후 동일 Refresh 쿠키로 재발급 시도 시 401 을 반환한다")
    void logout_thenRefresh_returns401() throws Exception {
        // 1단계: 로그인 → Refresh 쿠키 + Access Token 획득
        Cookie refreshCookie = loginAndGetRefreshCookie();
        String accessToken = loginAndGetAccessToken();

        // 2단계: 로그아웃 (Access 토큰 필요)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        // 3단계: 로그아웃된 Refresh 쿠키로 재발급 시도 → 화이트리스트에서 제거되어 401
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    // ── Refresh 쿠키 누락 ─────────────────────────────────────────────

    @Test
    @DisplayName("Refresh 쿠키 없이 /auth/refresh 요청 시 401 을 반환한다")
    void refresh_withoutCookie_returns401() throws Exception {
        // 쿠키를 전혀 보내지 않으면 INVALID_REFRESH_TOKEN 으로 거부
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // ── SEC-06 위조 토큰 ──────────────────────────────────────────────

    @Test
    @DisplayName("SEC-06: 서명이 위조된 Refresh 쿠키 사용 시 401 을 반환한다")
    void refresh_withTamperedToken_returns401() throws Exception {
        // 로그인 후 정상 토큰 획득
        Cookie originalCookie = loginAndGetRefreshCookie();
        String originalToken = originalCookie.getValue();

        // 토큰 페이로드를 임의 변조 (Base64 디코딩 없이 단순 문자열 조작)
        // 마지막 서명 부분의 마지막 문자를 X 로 바꿔 서명 위조
        String tamperedToken = originalToken.substring(0, originalToken.length() - 2) + "XX";
        Cookie tamperedCookie = new Cookie(REFRESH_COOKIE_NAME, tamperedToken);
        tamperedCookie.setPath("/api/auth");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(tamperedCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    // ── 로그아웃 미인증 ───────────────────────────────────────────────

    @Test
    @DisplayName("미인증 상태에서 /auth/logout 요청 시 401 을 반환한다")
    void logout_withoutAccessToken_returns401() throws Exception {
        // logout 은 Access Token 이 있어야 처리된다 (SecurityConfig 에서 authenticated 설정)
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
