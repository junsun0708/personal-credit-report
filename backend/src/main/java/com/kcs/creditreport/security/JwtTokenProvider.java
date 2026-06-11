package com.kcs.creditreport.security;

import com.kcs.creditreport.config.JwtProperties;
import com.kcs.creditreport.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 생성·파싱·검증 컴포넌트 (설계서 §5).
 *
 * <p>HMAC-SHA256(HS256) 대칭키 서명을 사용한다. 서명 키는 {@link JwtProperties#secret()}
 * (application.yml / 환경변수)에서 주입받으며, 코드에 하드코딩하지 않는다.
 *
 * <p>토큰 클레임 구조:
 * <ul>
 *   <li>{@code sub} : userId (Long → String) — 인증 주체 식별자</li>
 *   <li>{@code email} : 사용자 이메일 (편의 클레임, 권한 판단엔 미사용)</li>
 *   <li>{@code typ} : ACCESS | REFRESH — 토큰 종류 (혼용 방어)</li>
 *   <li>{@code iat}/{@code exp} : 발급/만료 시각</li>
 * </ul>
 *
 * <p>검증 실패는 호출자가 식별할 수 있도록 {@link JwtValidationException}(만료/위조 구분
 * 가능한 {@link ErrorCode} 포함)으로 변환해 던진다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey signingKey;
    private final long accessExpMs;
    private final long refreshExpMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        // HMAC 키 길이 검증: HS256 은 최소 256bit(32바이트) 키를 요구한다.
        // 더미/운영 모두 짧은 키로 기동되면 즉시 실패시켜 약한 서명 사용을 원천 차단한다.
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 은 HS256 서명을 위해 최소 32바이트(256bit) 이상이어야 합니다. "
                            + "현재 길이=" + keyBytes.length + "바이트");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpMs = jwtProperties.accessExpMs();
        this.refreshExpMs = jwtProperties.refreshExpMs();
    }

    /** Access Token 발급 (만료 15분 기본). */
    public String createAccessToken(Long userId, String email) {
        return createToken(userId, email, TokenType.ACCESS, accessExpMs);
    }

    /** Refresh Token 발급 (만료 7일 기본). */
    public String createRefreshToken(Long userId, String email) {
        return createToken(userId, email, TokenType.REFRESH, refreshExpMs);
    }

    private String createToken(Long userId, String email, TokenType type, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey) // HS256 (키 길이로 알고리즘 자동 선택)
                .compact();
    }

    /** Access 만료(초) — 응답 expiresIn 용도. */
    public long getAccessExpiresInSeconds() {
        return Duration.ofMillis(accessExpMs).toSeconds();
    }

    /** Refresh 만료(초) — 쿠키 Max-Age 용도. */
    public long getRefreshExpiresInSeconds() {
        return Duration.ofMillis(refreshExpMs).toSeconds();
    }

    /**
     * 토큰을 파싱·검증하고 클레임을 반환한다.
     *
     * @param token         JWT 문자열
     * @param expectedType  기대 토큰 종류(Access 경로면 ACCESS, Refresh 경로면 REFRESH)
     * @return 검증된 Claims
     * @throws JwtValidationException 만료(TOKEN_EXPIRED) / 위조·형식오류·종류불일치(UNAUTHORIZED 또는 INVALID_REFRESH_TOKEN)
     */
    public Claims parseAndValidate(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 토큰 종류 일치 검증 (Access ↔ Refresh 혼용 차단)
            String typ = claims.get(CLAIM_TYPE, String.class);
            if (typ == null || !typ.equals(expectedType.name())) {
                log.warn("토큰 종류 불일치: expected={}, actual={}", expectedType, typ);
                throw new JwtValidationException(errorCodeFor(expectedType, false));
            }
            return claims;

        } catch (ExpiredJwtException e) {
            // 만료는 클라이언트의 refresh 트리거 신호 — 별도 코드로 구분
            log.debug("토큰 만료: type={}", expectedType);
            throw new JwtValidationException(errorCodeFor(expectedType, true));
        } catch (JwtException | IllegalArgumentException e) {
            // 위조·서명불일치·형식오류·null/blank 등
            log.warn("토큰 검증 실패: type={}, reason={}", expectedType, e.getMessage());
            throw new JwtValidationException(errorCodeFor(expectedType, false));
        }
    }

    /** 클레임에서 userId(subject) 추출. */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 클레임에서 email 추출. */
    public String getEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * 토큰 종류·실패 사유에 맞는 에러 코드 선택.
     *
     * <p>Refresh 경로 실패는 만료/위조 구분 없이 모두 INVALID_REFRESH_TOKEN 으로 통일한다
     * (명세서 §3.3: 누락·만료·위변조·폐기 모두 401 INVALID_REFRESH_TOKEN).
     * Access 경로는 만료(TOKEN_EXPIRED)와 그 외(UNAUTHORIZED)를 구분해 FE 재발급 흐름을 돕는다.
     */
    private ErrorCode errorCodeFor(TokenType type, boolean expired) {
        if (type == TokenType.REFRESH) {
            return ErrorCode.INVALID_REFRESH_TOKEN;
        }
        return expired ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED;
    }
}
