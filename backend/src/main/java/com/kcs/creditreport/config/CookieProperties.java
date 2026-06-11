package com.kcs.creditreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh 쿠키 속성 바인딩 (application.yml 의 {@code auth.cookie.*}).
 *
 * <p>API 명세서 §4.2 의 쿠키 규약(HttpOnly·Secure·SameSite·Path·Max-Age)을 코드에서
 * 설정값으로 주입받아 일관되게 생성/삭제한다.
 *
 * @param name     쿠키 이름 (예: {@code refreshToken})
 * @param path     쿠키 경로 (예: {@code /api/auth} — 인증 엔드포인트로 범위 제한)
 * @param secure   Secure 속성 (HTTPS 한정). 로컬 http 개발 시 false 오버라이드 가능
 * @param sameSite SameSite 정책 (예: {@code Strict})
 */
@ConfigurationProperties(prefix = "auth.cookie")
public record CookieProperties(
        String name,
        String path,
        boolean secure,
        String sameSite
) {
}
