package com.kcs.creditreport.dto.auth;

import com.kcs.creditreport.domain.User;
import java.time.LocalDateTime;

/**
 * 회원가입 성공 응답 DTO (API 명세서 §3.1) — 201 Created.
 *
 * <p>비밀번호 해시는 절대 응답에 포함하지 않는다.
 *
 * @param id        생성된 회원 ID
 * @param email     가입 이메일
 * @param createdAt 가입 일시(ISO-8601)
 */
public record SignupResponse(
        Long id,
        String email,
        LocalDateTime createdAt
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
