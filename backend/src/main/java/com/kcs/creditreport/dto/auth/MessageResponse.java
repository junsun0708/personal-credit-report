package com.kcs.creditreport.dto.auth;

/**
 * 단순 메시지 응답 (예: 로그아웃 — API 명세서 §3.4).
 *
 * @param message 사용자용 한국어 메시지
 */
public record MessageResponse(String message) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
