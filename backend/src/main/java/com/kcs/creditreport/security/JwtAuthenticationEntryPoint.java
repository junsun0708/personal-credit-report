package com.kcs.creditreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcs.creditreport.dto.ErrorResponse;
import com.kcs.creditreport.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패(401) 진입점 (설계서 §5.3, §7.1).
 *
 * <p>SecurityContext 에 인증이 없는 채로 보호 리소스에 도달하면 Spring Security 가 호출한다.
 * 표준 {@code {code, message}} JSON 으로 401 을 직접 작성한다(전역 핸들러와 동일 포맷 유지).
 *
 * <p>필터({@link JwtAuthenticationFilter})가 만료/위조 사유를 request attribute 에 남겼다면
 * 그 코드(TOKEN_EXPIRED 등)를 사용하고, 없으면 기본 UNAUTHORIZED(토큰 미존재)로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        // 필터가 남긴 구체 사유(만료/위조) 우선, 없으면 UNAUTHORIZED 기본값.
        Object attr = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = (attr instanceof ErrorCode ec) ? ec : ErrorCode.UNAUTHORIZED;

        writeError(response, errorCode);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
