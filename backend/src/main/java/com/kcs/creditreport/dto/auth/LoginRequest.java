package com.kcs.creditreport.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO (API 명세서 §3.2).
 *
 * <p>형식 검증만 수행한다(비밀번호 정책 정규식은 적용하지 않음 — 기존 사용자의 비밀번호가
 * 정책 변경 전 값일 수 있고, 무엇보다 로그인 단계에서 정책 위반 메시지를 노출하면
 * 사용자 열거 단서가 될 수 있으므로 형식 검사에 그친다). 자격 검증은 서비스 계층에서.
 *
 * @param email    이메일 형식
 * @param password 비밀번호 평문(서버에서 BCrypt matches 검증 후 즉시 폐기)
 */
public record LoginRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
