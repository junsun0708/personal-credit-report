package com.kcs.creditreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcs.creditreport.dto.ErrorResponse;
import com.kcs.creditreport.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인가 실패(403) 핸들러 (설계서 §7.1).
 *
 * <p>인증은 되었으나 권한이 없는 요청에 대해 표준 {@code {code, message}} JSON 으로 403 을
 * 작성한다. (본 과제는 단일 사용자 권한 모델이라 403 발생 빈도는 낮지만, 필터 체인 일관성을
 * 위해 명시적으로 등록한다.)
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.FORBIDDEN));
    }
}
