package com.kcs.creditreport.security;

/**
 * 인증 주체 — JWT 에서 추출한 최소 식별 정보.
 *
 * <p>SecurityContext 의 Authentication principal 로 저장되며, 컨트롤러에서
 * {@code @AuthenticationPrincipal AuthPrincipal principal} 로 주입받아 {@code userId} 를
 * 권한 스코프(설계서 §2.2: 모든 도메인 쿼리에 user_id 강제)로 사용한다.
 *
 * @param userId 회원 ID (도메인 권한 스코프 기준)
 * @param email  이메일 (로깅·표시 편의)
 */
public record AuthPrincipal(Long userId, String email) {
}
