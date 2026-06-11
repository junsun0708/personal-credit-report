package com.kcs.creditreport.security;

import com.kcs.creditreport.config.CorsProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 필터 체인 구성 (설계서 §5.3, §7.3).
 *
 * <p>핵심 설계:
 * <ul>
 *   <li><b>Stateless</b>: 세션을 생성하지 않고({@code STATELESS}) JWT 로만 인증한다.</li>
 *   <li><b>경로 정책</b>: {@code /api/auth/**} · {@code /h2-console/**} 는 permitAll,
 *       그 외 모든 요청은 authenticated.</li>
 *   <li><b>JWT 필터</b>: {@link JwtAuthenticationFilter} 를
 *       {@code UsernamePasswordAuthenticationFilter} 앞에 배치해 Access 를 검증.</li>
 *   <li><b>예외 처리</b>: 인증 실패 → {@link JwtAuthenticationEntryPoint}(401),
 *       인가 실패 → {@link JwtAccessDeniedHandler}(403). 표준 {@code {code,message}} 포맷.</li>
 *   <li><b>CORS</b>: FE(:3000) 교차 출처 + 쿠키 자격증명 허용(allowCredentials=true).</li>
 * </ul>
 *
 * <p><b>CSRF disable 근거</b>: 본 API 는 상태 변경 요청의 인증을 <i>쿠키 세션이 아니라
 * Authorization 헤더의 Bearer Access Token</i> 으로 수행한다. CSRF 는 "브라우저가 쿠키를
 * 자동 동봉"하는 점을 악용하는 공격인데, 헤더 토큰은 공격자 사이트가 임의로 설정할 수 없으므로
 * 상태변경 API 는 구조적으로 CSRF 에 노출되지 않는다. 유일한 쿠키 인증 경로인
 * {@code /api/auth/refresh} 는 Refresh 쿠키를 {@code SameSite=Strict} 로 발급해
 * 교차 출처 자동 전송을 차단함으로써 CSRF 표면을 제거한다(설계서 §8). 따라서 CSRF 토큰
 * 메커니즘은 불필요하며, stateless API 와의 충돌을 피하기 위해 비활성화한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS: 쿠키 자격증명 송수신을 위해 명시적 출처 등록(와일드카드 불가)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 — 근거는 클래스 Javadoc 참조(헤더 Bearer + SameSite 쿠키로 방어)
                .csrf(AbstractHttpConfigurer::disable)

                // 기본 로그인 폼·HTTP Basic 비활성화(JWT 전용 인증)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 미사용 — 완전 stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // h2-console 은 <iframe> 으로 렌더되므로 동일 출처 프레임 허용 필요
                .headers(headers -> headers.frameOptions(
                        HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // 경로별 인가 규칙
                //  - signup/login/refresh : 비인증 허용(refresh 는 쿠키로 자체 인증)
                //  - logout : Access Token 필요(명세 §3.4) → 명시적으로 authenticated
                //    (permitAll 범위에 두면 EntryPoint 가 401 을 못 만들고 NPE 위험 → 분리)
                //  - h2-console : 개발 편의 permitAll
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())

                // 인증/인가 실패 → 표준 에러 포맷
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // JWT 검증 필터를 인증 필터 체인 앞단에 삽입
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정 (설계서 §7.3).
     *
     * <p>allowCredentials=true 이므로 출처를 와일드카드(*)로 둘 수 없고 명시 등록한다.
     * Refresh 쿠키 송수신을 위해 자격증명을 허용한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsProperties.allowedOrigin()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true); // Refresh 쿠키 전송 필수
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
