package com.kcs.creditreport.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-LIST 리포트 목록 서버처리 통합 테스트 (P0/P1)
 *
 * <p>검증 대상: GET /api/reports (인증 필요)
 * <ul>
 *   <li>TC-LS-01 : 기본 페이징(size=10) → 10건, totalElements=28</li>
 *   <li>TC-LS-02 : 2페이지 이동 → 10건</li>
 *   <li>TC-LS-05 : 등급 필터 → 해당 등급만 반환</li>
 *   <li>TC-LS-07 : 신용점수 내림차순 정렬 → 첫 번째 항목이 최고 점수</li>
 *   <li>TC-LS-08 : 신용점수 오름차순 정렬 → 첫 번째 항목이 최저 점수</li>
 *   <li>TC-CM-02 : PageResponse 표준 포맷 검증</li>
 * </ul>
 *
 * <p>시드 데이터 전제: user_id=1 (test@kcs.com) 소유 28건, 등급 1~10 분산
 */
@DisplayName("리포트 목록 서버처리 통합 테스트")
class ReportListIntegrationTest extends IntegrationTestBase {

    /** 로그인 토큰 캐시 — 매 테스트마다 재로그인 비용 절감 */
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = loginAndGetAccessToken();
    }

    // ── TC-LS-01 / TC-CM-02 ───────────────────────────────────────────

    @Test
    @DisplayName("TC-LS-01: 기본 size=10 으로 조회하면 10건이 반환되고 totalElements=28 이다")
    void getReports_defaultPaging_returns10ItemsWithTotal28() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("size", "10")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 현재 페이지 데이터 10건
                .andExpect(jsonPath("$.data", hasSize(10)))
                // 시드 전체 28건
                .andExpect(jsonPath("$.totalElements").value(28))
                // 10건 페이지이면 totalPages = ceil(28/10) = 3
                .andExpect(jsonPath("$.totalPages").value(3))
                // 페이지 번호는 1-based
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("TC-CM-02: 목록 응답이 PageResponse 표준 포맷(data/page/size/totalElements/totalPages)을 따른다")
    void getReports_responseMatchesPageResponseFormat() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 필수 필드 모두 존재해야 한다
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    // ── TC-LS-02 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-LS-02: page=2 로 조회하면 2페이지 데이터(최대 10건)가 반환된다")
    void getReports_page2_returnsNextPage() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("page", "2")
                        .param("size", "10")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                // 28건 중 2페이지: 10건 존재
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(28));
    }

    @Test
    @DisplayName("TC-LS-02-B: page=3 (마지막 페이지)에는 나머지 8건이 반환된다")
    void getReports_page3_returnsRemaining8Items() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("page", "3")
                        .param("size", "10")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                // 28 - 10 - 10 = 8건 남음
                .andExpect(jsonPath("$.data", hasSize(8)));
    }

    // ── TC-LS-05 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-LS-05: grade=1 필터 시 1등급 리포트만 반환된다")
    void getReports_gradeFilter_returnsOnlySpecifiedGrade() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("grade", "1")
                        .param("size", "100")  // 전체 확인
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 시드 데이터: 1등급 3건 (행 1,2,28)
                .andExpect(jsonPath("$.data[*].creditGrade")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo(1))));
    }

    @Test
    @DisplayName("TC-LS-05-B: grade=10 필터 시 10등급 리포트만 반환된다")
    void getReports_grade10Filter_returnsOnly10thGrade() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("grade", "10")
                        .param("size", "100")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].creditGrade")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo(10))));
    }

    // ── TC-LS-07/08 정렬 ──────────────────────────────────────────────

    @Test
    @DisplayName("TC-LS-07: creditScore desc 정렬 시 첫 번째 항목이 가장 높은 점수이다")
    void getReports_sortByCreditScoreDesc_firstItemHasHighestScore() throws Exception {
        var result = mockMvc.perform(get("/api/reports")
                        .param("sort", "creditScore")
                        .param("order", "desc")
                        .param("size", "100")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // 응답에서 점수 목록 추출하여 내림차순 정렬 검증
        var tree = objectMapper.readTree(result.getResponse().getContentAsString());
        var data = tree.get("data");
        int prevScore = Integer.MAX_VALUE;
        for (var item : data) {
            int score = item.get("creditScore").asInt();
            org.assertj.core.api.Assertions.assertThat(score)
                    .as("creditScore desc 정렬: 각 항목 점수가 이전 항목보다 작거나 같아야 한다")
                    .isLessThanOrEqualTo(prevScore);
            prevScore = score;
        }
    }

    @Test
    @DisplayName("TC-LS-08: creditScore asc 정렬 시 첫 번째 항목이 가장 낮은 점수이다")
    void getReports_sortByCreditScoreAsc_firstItemHasLowestScore() throws Exception {
        var result = mockMvc.perform(get("/api/reports")
                        .param("sort", "creditScore")
                        .param("order", "asc")
                        .param("size", "100")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();

        var tree = objectMapper.readTree(result.getResponse().getContentAsString());
        var data = tree.get("data");
        int prevScore = Integer.MIN_VALUE;
        for (var item : data) {
            int score = item.get("creditScore").asInt();
            org.assertj.core.api.Assertions.assertThat(score)
                    .as("creditScore asc 정렬: 각 항목 점수가 이전 항목보다 크거나 같아야 한다")
                    .isGreaterThanOrEqualTo(prevScore);
            prevScore = score;
        }
    }

    // ── 잘못된 파라미터 검증 ──────────────────────────────────────────

    @Test
    @DisplayName("grade 범위 초과(grade=11) 시 400 을 반환한다")
    void getReports_gradeOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("grade", "11")
                        .header("Authorization", accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("허용되지 않은 sort 파라미터 사용 시 400 을 반환한다")
    void getReports_invalidSortParam_returns400() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("sort", "unknown")
                        .header("Authorization", accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
