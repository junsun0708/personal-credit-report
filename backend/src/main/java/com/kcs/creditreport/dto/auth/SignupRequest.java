package com.kcs.creditreport.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO (API 명세서 §3.1).
 *
 * <p>서버 측에서 Bean Validation 으로 이메일 형식과 비밀번호 정책을 강제한다(보안 요건:
 * 클라이언트 검증에 의존하지 않고 서버가 최종 강제). 위반 시 400 VALIDATION_ERROR.
 *
 * @param email    이메일 형식, 미가입(중복은 서비스 계층에서 409 처리)
 * @param password 8자 이상, 영문/숫자/특수문자 조합
 */
public record SignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        // 비밀번호 정책: 최소 1개의 영문자·숫자·특수문자를 모두 포함(공백 불허).
        //  - (?=.*[A-Za-z]) : 영문자 1개 이상
        //  - (?=.*\d)       : 숫자 1개 이상
        //  - (?=.*[^A-Za-z0-9]) : 특수문자(영숫자 외) 1개 이상
        //  - 전체 8자 이상은 @Size 와 정규식 {8,} 으로 이중 보강
        // 상한(@Size max=64): BCrypt 72바이트 입력 한계 및 DoS(긴 입력 해싱) 방어 목적.
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,}$",
                message = "비밀번호는 8자 이상이며 영문·숫자·특수문자를 모두 포함해야 합니다."
        )
        String password
) {
}
