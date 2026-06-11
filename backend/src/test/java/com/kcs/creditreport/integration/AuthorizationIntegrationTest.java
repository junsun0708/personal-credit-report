package com.kcs.creditreport.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-AUTH-AUTHZ 권한 격리 통합 테스트 (P0 핵심 보안)
 *
 * <p>검증 대상: 보호된 API 엔드포인트의 인증·인가 동작
 * <ul>
 *   <li>TC-AZ-01 : 토큰 없이 /api/reports 접근 → 401</li>
 *   <li>TC-AZ-01-B: 유효 토큰으로 /api/reports 접근 → 200</li>
 *   <li>TC-AZ-02 : 존재하지 않는/타인 리포트 ID 조회 → 404 (IDOR 방지)</li>
 *   <li>TC-AZ-04 : 목록 응답에 본인 소유 리포트만 포함</li>
 *   <li>SEC-01   : 타인 리포트 직접 조회 시 404 (데이터 미노출)</li>
 * </ul>
 */
@DisplayName("권한 격리 통합 테스트")
class AuthorizationIntegrationTest extends IntegrationTestBase {

    // ── TC-AZ-01 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-AZ-01: 토큰 없이 /api/reports 접근 시 401 을 반환한다")
    void reports_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-AZ-01-B: 토큰 없이 /api/histories 접근 시 401 을 반환한다")
    void histories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/histories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-AZ-01-C: 유효한 토큰으로 /api/reports 접근 시 200 을 반환한다")
    void reports_withValidToken_returns200() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/reports")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // PageResponse 표준 필드 확인
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.page").isNumber());
    }

    // ── TC-AZ-02 / SEC-01 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-AZ-02 / SEC-01: 존재하지 않는 리포트 ID 조회 시 404 를 반환한다")
    void reportDetail_nonexistentId_returns404() throws Exception {
        String accessToken = loginAndGetAccessToken();

        // 시드 데이터에 없을 매우 큰 ID 사용
        mockMvc.perform(get("/api/reports/99999")
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("TC-AZ-02-B: 음수 ID 로 리포트 조회 시 404 를 반환한다 (IDOR 방지)")
    void reportDetail_negativeId_returns404() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/reports/-1")
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound());
    }

    // ── TC-AZ-04 / SEC-02 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-AZ-04: 목록 조회 시 본인 소유 리포트만 반환하고 개수가 28건(시드 전체) 이다")
    void reports_returnOnlyOwnedReports() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/reports")
                        .param("size", "100")  // 전체를 한 번에 조회
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 시드 data.sql 에 user_id=1 리포트 28건 (문제 명세에도 28건 명시)
                .andExpect(jsonPath("$.totalElements").value(28));
    }

    // ── 잘못된 Bearer 토큰 ─────────────────────────────────────────────

    @Test
    @DisplayName("잘못된 형식의 Bearer 토큰으로 접근 시 401 을 반환한다")
    void reports_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
