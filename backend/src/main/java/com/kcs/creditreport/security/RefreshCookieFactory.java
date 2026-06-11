package com.kcs.creditreport.security;

import com.kcs.creditreport.config.CookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Refresh Token 쿠키 생성/삭제 팩토리 (API 명세서 §4.2).
 *
 * <p>쿠키 속성(이름·Path·Secure·SameSite)을 {@link CookieProperties} 로부터 일관 적용한다.
 * <ul>
 *   <li>{@code HttpOnly=true} : JS 접근 차단(XSS 로 탈취 불가)</li>
 *   <li>{@code Secure} : 설정값(운영 true). HTTPS 전송 한정</li>
 *   <li>{@code SameSite=Strict} : 교차 출처 자동 전송 차단(CSRF 표면 제거)</li>
 *   <li>{@code Path=/api/auth} : 인증 엔드포인트로 범위 제한(일반 API 에 미동봉)</li>
 *   <li>{@code Max-Age} : Refresh 만료(초). 삭제 시 0</li>
 * </ul>
 */
@Component
public class RefreshCookieFactory {

    private final CookieProperties cookieProperties;

    public RefreshCookieFactory(CookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    /** 발급용 Refresh 쿠키 생성. */
    public ResponseCookie create(String refreshToken, long maxAgeSeconds) {
        return baseBuilder(refreshToken, maxAgeSeconds).build();
    }

    /** 삭제용 쿠키 생성(빈 값 + Max-Age=0). 동일 Path/이름이어야 브라우저가 제거한다. */
    public ResponseCookie delete() {
        return baseBuilder("", 0).build();
    }

    /** Refresh 쿠키 이름 — 컨트롤러가 요청 쿠키 추출 시 사용. */
    public String cookieName() {
        return cookieProperties.name();
    }

    /** 요청에서 Refresh 토큰 값 추출(없으면 null). */
    public String resolve(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (cookieProperties.name().equals(cookie.getName())) {
                String value = cookie.getValue();
                return StringUtils.hasText(value) ? value : null;
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path())
                .maxAge(maxAgeSeconds);
    }
}
