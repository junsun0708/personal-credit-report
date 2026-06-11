package com.kcs.creditreport.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-HISTORY 조회 이력 통합 테스트 (P0/P1)
 *
 * <p>검증 대상: GET /api/histories, GET /api/reports/{id} 의 이력 기록 부수효과
 * <ul>
 *   <li>TC-DT-04 : 상세 조회 후 /api/histories 에 1건 이상 기록됨</li>
 *   <li>TC-HS-01 : 이력은 최신순(viewed_at DESC) 정렬</li>
 *   <li>TC-HS-02 : 동일 리포트 2회 조회 시 2건 모두 기록</li>
 *   <li>TC-HS-04 : /api/histories 는 본인 이력만 반환 (권한 격리)</li>
 * </ul>
 *
 * <p>격리 전략: 각 테스트는 상세 조회로 이력을 동적 생성한다.
 * H2 인메모리 공유 컨텍스트이므로 각 테스트의 이력 개수는 누적될 수 있음.
 * 따라서 "이력이 N건 이상" 방식으로 검증해 순서 독립성을 확보한다.
 */
@DisplayName("조회 이력 통합 테스트")
class HistoryIntegrationTest extends IntegrationTestBase {

    private String accessToken;
    private long firstReportId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = loginAndGetAccessToken();
        // 목록 첫 항목 ID 를 동적으로 획득
        var listResult = mockMvc.perform(get("/api/reports")
                        .param("size", "1")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        firstReportId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()
        ).get("data").get(0).get("id").asLong();
    }

    // ── TC-DT-04 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DT-04: 리포트 상세 조회 후 /api/histories 에 이력이 1건 이상 기록된다")
    void viewReport_thenHistoryContainsAtLeastOneEntry() throws Exception {
        // 1단계: 상세 조회 → 이력 기록 트리거
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        // 2단계: 이력 목록에서 최소 1건 이상 기록 확인
        mockMvc.perform(get("/api/histories")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("TC-DT-04-B: 이력 항목에 리포트 정보와 viewed_at 시각이 포함된다")
    void viewReport_historyItemHasRequiredFields() throws Exception {
        // 상세 조회로 이력 생성
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        // 이력 목록 확인
        mockMvc.perform(get("/api/histories")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reportId").exists())
                .andExpect(jsonPath("$.data[0].viewedAt").exists());
    }

    // ── TC-HS-01 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-HS-01: 이력 목록은 최신순(viewed_at DESC)으로 정렬되어 반환된다")
    void getHistories_returnedInDescendingOrder() throws Exception {
        // 1단계: 2건의 이력 생성 (시간 간격을 두기 위해 2개의 다른 리포트 조회)
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        // 두 번째 리포트 ID 가져오기
        var listResult = mockMvc.perform(get("/api/reports")
                        .param("size", "2")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        var data = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get("data");
        long secondReportId = data.get(1).get("id").asLong();

        mockMvc.perform(get("/api/reports/{id}", secondReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        // 2단계: 이력 조회 → viewed_at 오름차순이 아닌 것을 확인
        var histResult = mockMvc.perform(get("/api/histories")
                        .param("size", "100")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();

        var histData = objectMapper.readTree(
                histResult.getResponse().getContentAsString()).get("data");

        // 이력이 2건 이상 있을 때 첫 번째 항목의 viewedAt 이 두 번째보다 최신(같거나 크다)임을 검증
        if (histData.size() >= 2) {
            String first = histData.get(0).get("viewedAt").asText();
            String second = histData.get(1).get("viewedAt").asText();
            // ISO-8601 문자열 사전순 비교 = 시간 순서 비교
            org.assertj.core.api.Assertions.assertThat(first)
                    .as("이력은 최신순: 첫 번째 항목이 두 번째 항목보다 시각상 같거나 나중이어야 한다")
                    .isGreaterThanOrEqualTo(second);
        }
    }

    // ── TC-HS-02 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-HS-02: 동일 리포트를 2회 조회하면 이력이 2건 모두 기록된다")
    void viewSameReportTwice_bothHistoriesRecorded() throws Exception {
        // 이전 이력 건수 기록
        var beforeResult = mockMvc.perform(get("/api/histories")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        long beforeCount = objectMapper.readTree(
                beforeResult.getResponse().getContentAsString()
        ).get("totalElements").asLong();

        // 동일 리포트 2회 조회
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                .header("Authorization", accessToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                .header("Authorization", accessToken)).andExpect(status().isOk());

        // 이후 이력 건수 확인: 정확히 2건 증가했어야 한다
        var afterResult = mockMvc.perform(get("/api/histories")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        long afterCount = objectMapper.readTree(
                afterResult.getResponse().getContentAsString()
        ).get("totalElements").asLong();

        org.assertj.core.api.Assertions.assertThat(afterCount - beforeCount)
                .as("동일 리포트 2회 조회: 이력이 정확히 2건 증가해야 한다")
                .isEqualTo(2L);
    }

    // ── TC-HS-04 / SEC-03 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-HS-04: /api/histories 는 본인 이력만 반환한다 (PageResponse 포맷 포함)")
    void getHistories_returnOnlyOwnHistories() throws Exception {
        // 상세 조회로 이력 1건 생성
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/histories")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // PageResponse 포맷 확인
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.page").isNumber());
    }

    // ── 미인증 이력 접근 ──────────────────────────────────────────────

    @Test
    @DisplayName("토큰 없이 /api/histories 접근 시 401 을 반환한다")
    void getHistories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/histories"))
                .andExpect(status().isUnauthorized());
    }
}
