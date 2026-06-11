package com.kcs.creditreport.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통 기반 클래스.
 *
 * <p>모든 통합 테스트는 이 클래스를 상속해 MockMvc 와 공통 헬퍼를 재사용한다.
 *
 * <p>설정 오버라이드:
 * <ul>
 *   <li>COOKIE_SECURE=false : H2 테스트 환경은 HTTP 이므로 Secure 쿠키 비활성화</li>
 *   <li>JWT_ACCESS_EXP=900000 : 기본값 유지(테스트는 시드 토큰 사용)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // H2 인메모리 환경은 HTTP 이므로 Secure 쿠키 속성을 false 로 오버라이드
        "COOKIE_SECURE=false"
})
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── 시드 계정 상수 (data.sql §7.1 동기화) ─────────────────────────
    /** 주 테스트 계정 이메일 (시드 data.sql) */
    protected static final String SEED_EMAIL = "test@kcs.com";
    /** 주 테스트 계정 비밀번호 (시드 data.sql) */
    protected static final String SEED_PASSWORD = "Test1234!";
    /** Refresh 쿠키 이름 (application.yml auth.cookie.name) */
    protected static final String REFRESH_COOKIE_NAME = "refreshToken";

    // ── 공통 헬퍼 메서드 ──────────────────────────────────────────────

    /**
     * 시드 계정으로 로그인하고 Access Token 을 반환한다.
     *
     * @return Bearer 접두어를 포함한 Authorization 헤더 값
     */
    protected String loginAndGetAccessToken() throws Exception {
        return loginAndGetAccessToken(SEED_EMAIL, SEED_PASSWORD);
    }

    /**
     * 지정 자격증명으로 로그인하고 Access Token 을 반환한다.
     *
     * @param email    이메일
     * @param password 비밀번호
     * @return Bearer 접두어를 포함한 Authorization 헤더 값
     */
    protected String loginAndGetAccessToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("email", email, "password", password));

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        return "Bearer " + accessToken;
    }

    /**
     * 시드 계정으로 로그인하고 Refresh 쿠키를 반환한다.
     *
     * @return refreshToken 쿠키 객체
     */
    protected Cookie loginAndGetRefreshCookie() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("email", SEED_EMAIL, "password", SEED_PASSWORD));

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie refreshCookie = response.getCookie(REFRESH_COOKIE_NAME);
        if (refreshCookie == null) {
            throw new AssertionError("로그인 응답에 refreshToken 쿠키가 없습니다.");
        }
        return refreshCookie;
    }

    /**
     * 간단한 JSON 요청 바디를 생성하는 유틸.
     *
     * @param pairs key-value 쌍 (홀수 인덱스=키, 짝수=값)
     * @return JSON 문자열
     */
    protected String json(String... pairs) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }
}
