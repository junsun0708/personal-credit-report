package com.kcs.creditreport.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 화이트리스트 (인메모리) — 설계서 §5.5.
 *
 * <p>발급된 Refresh Token 을 사용자별로 보관하고, 로그아웃 시 제거(폐기)한다. 재발급·검증
 * 시 화이트리스트 존재 여부를 함께 확인하여, 만료 전이라도 <b>서버 측에서 무효화</b>할 수 있게 한다
 * (JWT 자체는 stateless 라 서명만으로는 폐기 불가 — 이를 보완).
 *
 * <p><b>한계(주석 명시)</b>: 인메모리 {@link ConcurrentHashMap} 저장이므로 서버 재기동 시
 * 화이트리스트가 초기화된다. 그 경우 기존 Refresh 토큰은 (서명·만료가 유효해도) 화이트리스트에
 * 없어 재발급이 거부되어 재로그인이 필요하다. 운영에서는 Redis 등 외부 저장소로 대체해야 한다.
 * (README 회고에 기록)
 *
 * <p>구현 단순화를 위해 "사용자당 1개의 활성 Refresh" 모델을 사용한다(최근 로그인이 이전 토큰을
 * 덮어씀 → 다중 기기 동시 세션은 미지원, 과제 범위상 충분).
 */
@Component
public class RefreshTokenStore {

    /** key = userId, value = 현재 유효한 Refresh Token 문자열. */
    private final ConcurrentMap<Long, String> store = new ConcurrentHashMap<>();

    /** 로그인/회전 시 사용자의 활성 Refresh 토큰을 저장(기존 값 대체). */
    public void store(Long userId, String refreshToken) {
        store.put(userId, refreshToken);
    }

    /**
     * 제시된 Refresh 토큰이 해당 사용자의 현재 활성 토큰과 일치하는지 검증.
     *
     * @return 화이트리스트에 존재하고 값이 정확히 일치하면 true
     */
    public boolean isValid(Long userId, String refreshToken) {
        String stored = store.get(userId);
        return stored != null && stored.equals(refreshToken);
    }

    /** 로그아웃/회전 폐기 시 사용자의 활성 Refresh 토큰 제거. */
    public void remove(Long userId) {
        store.remove(userId);
    }
}
