package com.kcs.creditreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 설정값 바인딩 (application.yml 의 {@code auth.cors.*}, 설계서 §7.3).
 *
 * @param allowedOrigin 허용 출처(FE). 쿠키 자격증명 사용으로 와일드카드 불가 → 명시 등록.
 */
@ConfigurationProperties(prefix = "auth.cors")
public record CorsProperties(
        String allowedOrigin
) {
}
