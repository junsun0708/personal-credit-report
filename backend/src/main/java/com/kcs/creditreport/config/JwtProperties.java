package com.kcs.creditreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정값 바인딩 (application.yml 의 {@code jwt.*}).
 *
 * <p>시크릿·만료시간을 코드에 하드코딩하지 않고 외부 설정/환경변수로 주입한다(설계서 §7.4).
 * 운영 환경에서는 {@code JWT_SECRET} 환경변수로 더미 시크릿을 반드시 오버라이드한다.
 *
 * @param secret      HMAC-SHA256 서명 키 (최소 256bit/32바이트 권장)
 * @param accessExpMs Access Token 만료(ms). 기본 900,000(15분)
 * @param refreshExpMs Refresh Token 만료(ms). 기본 604,800,000(7일)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpMs,
        long refreshExpMs
) {
}
