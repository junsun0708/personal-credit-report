package com.kcs.creditreport.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-AUTH-SIGNUP 통합 테스트 (P0 우선)
 *
 * <p>검증 대상: POST /api/auth/signup
 * <ul>
 *   <li>TC-AS-01 : 정상 회원가입 → 201</li>
 *   <li>TC-AS-02 : 이메일 형식 오류 → 400</li>
 *   <li>TC-AS-03 : 비밀번호 8자 미만 → 400</li>
 *   <li>TC-AS-04 : 비밀번호 조합 미충족(영문만) → 400</li>
 *   <li>TC-AS-05 : 이메일 중복 → 409</li>
 *   <li>TC-AS-06 : 필수값 누락 → 400</li>
 * </ul>
 */
@DisplayName("회원가입 통합 테스트")
class AuthSignupIntegrationTest extends IntegrationTestBase {

    // ── TC-AS-01 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-01: 정상 회원가입 시 201 Created 와 사용자 정보를 반환한다")
    void signup_success_returns201() throws Exception {
        // 시드 계정과 겹치지 않는 신규 이메일 사용
        String uniqueEmail = "new_user_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", uniqueEmail, "password", "Passw0rd!")))
                .andExpect(status().isCreated())
                // 응답에 이메일이 포함되어야 한다
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                // userId 가 생성되어야 한다
                .andExpect(jsonPath("$.id").exists())
                // 평문 비밀번호가 응답에 포함되면 안 된다
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── TC-AS-02 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-02: 이메일 형식이 올바르지 않으면 400 을 반환한다")
    void signup_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "invalid-email", "password", "Passw0rd!")))
                .andExpect(status().isBadRequest())
                // 표준 에러 포맷: code 필드 존재
                .andExpect(jsonPath("$.code").exists());
    }

    // ── TC-AS-03 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-03: 비밀번호가 8자 미만이면 400 을 반환한다")
    void signup_passwordTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        // 7자: 길이 정책 위반
                        .content(json("email", "short@test.com", "password", "Pa0!abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    // ── TC-AS-04 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-04: 비밀번호가 영문만 포함(숫자·특수문자 없음)이면 400 을 반환한다")
    void signup_passwordMissingDigitAndSpecial_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        // 영문만, 숫자와 특수문자 없음 → 정책 위반
                        .content(json("email", "combo@test.com", "password", "password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("TC-AS-04-B: 비밀번호가 영문+숫자만(특수문자 없음)이면 400 을 반환한다")
    void signup_passwordMissingSpecialChar_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        // 특수문자 미포함 → 정책 위반
                        .content(json("email", "nospecial@test.com", "password", "Password1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    // ── TC-AS-05 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-05: 이미 가입된 이메일로 회원가입 시 409 Conflict 를 반환한다")
    void signup_duplicateEmail_returns409() throws Exception {
        // 시드에 이미 존재하는 test@kcs.com 사용
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", SEED_EMAIL, "password", "Passw0rd!")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"));
    }

    // ── TC-AS-06 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AS-06: 이메일이 빈 값이면 400 을 반환한다")
    void signup_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "", "password", "Passw0rd!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("TC-AS-06-B: 비밀번호가 빈 값이면 400 을 반환한다")
    void signup_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "valid@test.com", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }
}
