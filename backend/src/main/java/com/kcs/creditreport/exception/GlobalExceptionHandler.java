package com.kcs.creditreport.exception;

import com.kcs.creditreport.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기 (설계서 §7.1, API 명세서 §1.5).
 *
 * <p>모든 컨트롤러 예외를 가로채 표준 {@code {code, message}} 포맷 + 적절한 HTTP 상태로
 * 변환한다. 내부 상세(스택트레이스 등)는 클라이언트로 노출하지 않고 서버 로깅만 수행한다.
 *
 * <p>주의: Spring Security 필터 단계(컨트롤러 진입 전)에서 발생하는 인증/인가 실패는
 * 이 핸들러로 오지 않는다. 그 경로는 {@code JwtAuthenticationEntryPoint} /
 * {@code JwtAccessDeniedHandler} 가 동일 포맷으로 처리한다(SecurityConfig 참조).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외(EmailDuplicated·InvalidCredentials·InvalidRefreshToken 등). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        // 4xx 비즈니스 예외는 운영상 정상 흐름이므로 WARN 레벨(스택트레이스 미출력)로 기록
        log.warn("BusinessException: code={}, message={}", code.name(), ex.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, ex.getMessage()));
    }

    /**
     * Bean Validation(@Valid) 실패 → 400 VALIDATION_ERROR.
     *
     * <p>첫 번째 필드 에러 메시지를 대표 메시지로 사용한다(클라이언트는 code 로 분기,
     * message 로 표시). 필드명을 포함해 디버깅 편의를 높이되 값(value)은 노출하지 않는다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = (firstError != null)
                ? "[" + firstError.getField() + "] " + firstError.getDefaultMessage()
                : ErrorCode.VALIDATION_ERROR.getDefaultMessage();
        log.warn("ValidationException: {}", message);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    /**
     * 컨트롤러 진입 이후 발생한 Spring Security 인증 예외 → 401 UNAUTHORIZED.
     * (대부분은 EntryPoint 가 선처리하지만, 방어적으로 매핑한다.)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED));
    }

    /** 인가 실패(권한 부족) → 403 FORBIDDEN. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN));
    }

    /** 그 외 모든 예외 → 500 INTERNAL_ERROR. 상세는 서버 로깅만, 클라이언트 비노출. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex); // 스택트레이스 서버 로깅
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}
