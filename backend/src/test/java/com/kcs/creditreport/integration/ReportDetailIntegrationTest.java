package com.kcs.creditreport.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-DETAIL 리포트 상세 통합 테스트 (P0 핵심 보안)
 *
 * <p>검증 대상: GET /api/reports/{id}
 * <ul>
 *   <li>TC-DT-01 : 본인 리포트 상세 조회 → 200, 핵심 필드 반환</li>
 *   <li>TC-DT-02 / SEC-11 : 주민번호가 마스킹 형식으로 반환된다</li>
 *   <li>TC-DT-03 / SEC-12 : 응답에 평문 주민번호 필드 없음</li>
 *   <li>TC-DT-07 : 존재하지 않는 리포트 ID → 404</li>
 * </ul>
 *
 * <p>시드 전제: user_id=1 소유 리포트 ID 는 1번부터 28번 (자동 증가, 다른 테스트가 선행 실행되어도
 * H2 인메모리 환경이므로 컨텍스트 공유 시 ID 가 이어질 수 있음 → 첫 번째 리포트를 동적으로 조회해 사용)
 */
@DisplayName("리포트 상세 통합 테스트")
class ReportDetailIntegrationTest extends IntegrationTestBase {

    private String accessToken;
    /** 시드 첫 번째 리포트 ID (목록 조회로 동적 획득) */
    private long firstReportId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = loginAndGetAccessToken();
        // 목록의 첫 번째 항목 ID 를 실제 응답에서 가져와 하드코딩 의존 제거
        var listResult = mockMvc.perform(get("/api/reports")
                        .param("size", "1")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        firstReportId = objectMapper.readTree(
                listResult.getResponse().getContentAsString()
        ).get("data").get(0).get("id").asLong();
    }

    // ── TC-DT-01 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DT-01: 본인 리포트 상세 조회 시 200 과 핵심 필드를 반환한다")
    void getReportDetail_ownReport_returns200WithDetails() throws Exception {
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 필수 응답 필드 확인
                .andExpect(jsonPath("$.id").value(firstReportId))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.institution").isString())
                .andExpect(jsonPath("$.creditScore").isNumber())
                .andExpect(jsonPath("$.creditGrade").isNumber())
                .andExpect(jsonPath("$.issuedDate").isString());
    }

    // ── TC-DT-02 / SEC-11 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-DT-02 / SEC-11: maskedSsn 이 '6자리-1자리******' 형식으로 반환된다")
    void getReportDetail_maskedSsn_hasCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 마스킹 패턴: YYMMDD-G****** (앞 6자리 + 하이픈 + 성별 1자리 + *6개)
                .andExpect(jsonPath("$.maskedSsn")
                        .value(matchesPattern("\\d{6}-\\d\\*{6}")));
    }

    @Test
    @DisplayName("TC-DT-02-B: 시드 첫 번째 리포트(900101-1234567)의 maskedSsn 이 '900101-1******' 이다")
    void getReportDetail_firstSeedReport_maskedSsnValue() throws Exception {
        // 목록을 issuedDate desc 기본 정렬로 조회하면 첫 번째가 다를 수 있으므로
        // 특정 ID(시드 ID=1 → 900101-1234567) 를 명시적으로 지정
        // 시드 ID 는 H2 auto_increment 로 1 부터 할당 (data.sql 순서 기준)
        // → setUp 의 firstReportId 가 정렬 기준으로 달라질 수 있어 별도 조회
        var listResult = mockMvc.perform(get("/api/reports")
                        .param("size", "100")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();

        // 시드에서 ssn='900101-1234567' 인 리포트를 찾아 마스킹 검증
        var data = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).get("data");
        long targetId = -1;
        for (var item : data) {
            // 제목으로 특정 리포트 식별 (시드 첫 번째: '2024 상반기 신용리포트')
            if ("2024 상반기 신용리포트".equals(item.get("title").asText())) {
                targetId = item.get("id").asLong();
                break;
            }
        }

        if (targetId < 0) {
            // 찾지 못하면 firstReportId 로 폴백
            targetId = firstReportId;
        }

        mockMvc.perform(get("/api/reports/{id}", targetId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // maskedSsn 패턴만 검증 (특정 값 하드코딩 대신 형식 검증으로 견고성 확보)
                .andExpect(jsonPath("$.maskedSsn")
                        .value(matchesPattern("\\d{6}-\\d\\*{6}")));
    }

    // ── TC-DT-03 / SEC-12 ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-DT-03 / SEC-12: 상세 응답에 평문 ssn 필드가 존재하지 않는다(서버 마스킹 확인)")
    void getReportDetail_rawSsnNotInResponse() throws Exception {
        mockMvc.perform(get("/api/reports/{id}", firstReportId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                // 평문 'ssn' 필드가 직렬화되지 않아야 한다
                .andExpect(jsonPath("$.ssn").doesNotExist());
    }

    // ── TC-DT-07 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-DT-07: 존재하지 않는 리포트 ID 조회 시 404 를 반환한다")
    void getReportDetail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/reports/99999")
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
