package com.kcs.creditreport.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-AUTH-LOGIN 통합 테스트 (P0 우선)
 *
 * <p>검증 대상: POST /api/auth/login
 * <ul>
 *   <li>TC-AL-01 : 정상 로그인 → 200, accessToken(body) + refreshToken 쿠키</li>
 *   <li>TC-AL-02 : 잘못된 비밀번호 → 401 (계정 존재 여부 미노출)</li>
 *   <li>TC-AL-03 : 미존재 이메일 → 401 (동일 메시지로 열거 방지)</li>
 *   <li>TC-AL-04 : Refresh 쿠키 속성(HttpOnly, SameSite) 검증</li>
 * </ul>
 */
@DisplayName("로그인 통합 테스트")
class AuthLoginIntegrationTest extends IntegrationTestBase {

    // ── TC-AL-01 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AL-01: 올바른 자격증명으로 로그인 시 200, accessToken 과 refreshToken 쿠키를 반환한다")
    void login_success_returns200WithTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", SEED_PASSWORD)))
                .andExpect(status().isOk())
                // 응답 바디에 accessToken 이 존재해야 한다
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                // tokenType 은 "Bearer" 여야 한다
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // expiresIn 이 양수여야 한다
                .andExpect(jsonPath("$.expiresIn").isNumber())
                // 사용자 이메일이 포함되어야 한다
                .andExpect(jsonPath("$.user.email").value(SEED_EMAIL))
                // Refresh 쿠키가 설정되어야 한다 (TC-AL-04 와 함께)
                .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE_NAME, true));
    }

    @Test
    @DisplayName("TC-AL-01-B: 로그인 성공 시 응답 바디에 refreshToken 이 노출되지 않는다")
    void login_success_refreshTokenNotInBody() throws Exception {
        // Refresh 토큰은 HttpOnly 쿠키로만 전달되어야 한다 (XSS 탈취 차단)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    // ── TC-AL-02 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AL-02: 잘못된 비밀번호로 로그인 시 401 을 반환한다")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", "WrongPass1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ── TC-AL-03 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AL-03: 존재하지 않는 이메일로 로그인 시 401 과 동일 메시지를 반환한다(열거 방지)")
    void login_nonexistentEmail_returns401WithSameMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "notexist@test.com", "password", "Passw0rd!")))
                .andExpect(status().isUnauthorized())
                // 이메일 존재 여부를 구분할 수 없는 동일한 에러 코드여야 한다
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                // 메시지도 동일한 일반화 문구여야 한다 (계정 열거 방지)
                .andExpect(jsonPath("$.message")
                        .value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("TC-AL-03-B: 미존재 이메일 401 메시지가 잘못된 비밀번호 401 메시지와 동일하다(열거 방지 검증)")
    void login_wrongPassword_and_nonexistentEmail_return_sameMessage() throws Exception {
        // 잘못된 비밀번호 메시지 추출
        var wrongPassResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", "WrongPass1!")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // 미존재 이메일 메시지 추출
        var noUserResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "ghost@test.com", "password", "Passw0rd!")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String msgWrong = objectMapper.readTree(wrongPassResult.getResponse().getContentAsString())
                .get("message").asText();
        String msgNoUser = objectMapper.readTree(noUserResult.getResponse().getContentAsString())
                .get("message").asText();

        // 두 메시지가 동일해야 한다 (사용자 열거 불가)
        org.assertj.core.api.Assertions.assertThat(msgWrong).isEqualTo(msgNoUser);
    }

    // ── TC-AL-04 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AL-04: Refresh 쿠키에 HttpOnly 속성이 설정되어 있어야 한다")
    void login_refreshCookie_isHttpOnly() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(REFRESH_COOKIE_NAME, true));
    }

    @Test
    @DisplayName("TC-AL-04-B: Refresh 쿠키 Set-Cookie 헤더에 SameSite=Strict 가 포함되어야 한다")
    void login_refreshCookie_hasSameSiteStrict() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        // MockMvc 의 cookie() 매처는 SameSite 를 직접 검증하지 못하므로 Set-Cookie 헤더 원문을 확인한다
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        org.assertj.core.api.Assertions.assertThat(setCookieHeader)
                .as("Refresh 쿠키에 SameSite=Strict 가 포함되어야 한다")
                .containsIgnoringCase("SameSite=Strict");
    }

    @Test
    @DisplayName("TC-AL-04-C: Refresh 쿠키 Path 가 /api/auth 로 제한되어야 한다")
    void login_refreshCookie_pathIsApiAuth() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().path(REFRESH_COOKIE_NAME, "/api/auth"));
    }
}
