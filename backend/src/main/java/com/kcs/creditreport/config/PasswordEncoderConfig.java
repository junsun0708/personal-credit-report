package com.kcs.creditreport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더 빈 설정.
 *
 * <p>{@link BCryptPasswordEncoder} 는 솔트를 해시 문자열 내부에 포함하는 단방향 적응형 해시로,
 * 비밀번호 평문을 절대 저장하지 않는다(보안 요건). 기본 강도(strength=10)를 사용한다.
 *
 * <p>{@code SecurityConfig} 와 분리해 둔 이유: 순환 의존 방지.
 * {@code AuthService} → {@code PasswordEncoder} 의존이 SecurityConfig 의 빈 초기화와
 * 얽히지 않도록 인코더만 독립 설정으로 노출한다.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 비밀번호 인코더.
     *
     * <p>data.sql 시드의 {@code test@kcs.com} 계정 해시도 동일한 BCrypt($2a$) 포맷이므로
     * {@code matches("Test1234!", storedHash)} 로 로그인 검증이 성립한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
