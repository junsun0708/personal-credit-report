package com.kcs.creditreport.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access Token 검증 필터 (설계서 §5.3).
 *
 * <p>매 요청마다 1회 실행({@link OncePerRequestFilter})되며 다음을 수행한다.
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} 헤더에서 Access Token 추출</li>
 *   <li>{@link JwtTokenProvider} 로 서명·만료·종류(ACCESS) 검증</li>
 *   <li>유효하면 {@link AuthPrincipal} 을 담은 인증 객체를 SecurityContext 에 설정</li>
 * </ol>
 *
 * <p>설계 원칙: 이 필터는 토큰이 없거나 무효여도 <b>예외를 던지지 않고 그대로 통과</b>시킨다.
 * 인증되지 않은 채로 보호 리소스에 도달하면 인가 단계에서 거부되고
 * {@link JwtAuthenticationEntryPoint} 가 401 을 생성한다(관심사 분리).
 * 단, 만료/위조 사유를 EntryPoint 가 구분해 응답할 수 있도록 request attribute 에 코드를 남긴다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "AUTH_ERROR_CODE";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(token, TokenType.ACCESS);
                AuthPrincipal principal = new AuthPrincipal(
                        jwtTokenProvider.getUserId(claims),
                        jwtTokenProvider.getEmail(claims)
                );
                // 권한(authorities)은 본 과제 범위상 단일 ROLE 불필요 → 빈 권한 + 인증됨 상태로 설정.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, Collections.emptyList());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtValidationException e) {
                // 인증 실패 사유를 EntryPoint 가 정확히 응답하도록 코드 전달 후 통과.
                // SecurityContext 는 비워둔 상태이므로 보호 리소스 접근 시 401 로 거부된다.
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }
        // 토큰 미존재 시: 미인증 상태로 진행. permitAll 경로면 그대로 통과,
        // 보호 경로면 EntryPoint 가 기본 UNAUTHORIZED 로 응답.

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더에서 Bearer 토큰만 추출(대소문자 prefix 엄격 검사). */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
