package com.kcs.creditreport.service;

import com.kcs.creditreport.domain.User;
import com.kcs.creditreport.dto.auth.SignupResponse;
import com.kcs.creditreport.exception.EmailDuplicatedException;
import com.kcs.creditreport.exception.InvalidCredentialsException;
import com.kcs.creditreport.exception.InvalidRefreshTokenException;
import com.kcs.creditreport.repository.UserRepository;
import com.kcs.creditreport.security.JwtTokenProvider;
import com.kcs.creditreport.security.RefreshTokenStore;
import com.kcs.creditreport.security.TokenType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직 (회원가입·로그인·재발급·로그아웃).
 *
 * <p>HTTP/쿠키 관심사는 다루지 않고(설계서 §2.2), 도메인 규칙·트랜잭션 경계·토큰 발급만
 * 담당한다. 생성된 토큰 원문은 {@link AuthTokens} 로 컨트롤러에 반환한다.
 *
 * <p>보안 결정:
 * <ul>
 *   <li>회원가입: 이메일 중복은 409, 비밀번호는 BCrypt 해시 저장(평문 미저장).</li>
 *   <li>로그인: 이메일 미존재와 비밀번호 불일치를 <b>동일 예외</b>로 처리해 사용자 열거 방지.
 *       또한 미존재 케이스에도 더미 해시로 BCrypt 검증을 수행해 응답 시간 차이를 줄인다
 *       (타이밍 기반 열거 완화).</li>
 *   <li>재발급/로그아웃: Refresh 화이트리스트로 서버 측 무효화를 강제.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 존재하지 않는 사용자에 대해서도 일정한 검증 비용을 발생시키기 위한 더미 BCrypt 해시.
     * (임의 평문의 해시. 실제 인증에는 절대 매칭되지 않음 — 타이밍 공격 완화용.)
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /** 회원가입 — 이메일 중복 검증 후 BCrypt 해시 저장. */
    @Transactional
    public SignupResponse signup(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            // 보안: 회원가입은 의도적으로 중복을 알린다(가입 UX 필수). 로그인과 달리 열거 위험보다
            // 사용성이 우선. 단, 다른 엔드포인트(로그인)에서는 열거를 차단한다.
            throw new EmailDuplicatedException();
        }
        String encoded = passwordEncoder.encode(rawPassword);
        User saved = userRepository.save(User.of(email, encoded));
        log.info("회원가입 완료: userId={}", saved.getId()); // 이메일 평문 로그 지양(id만)
        return SignupResponse.from(saved);
    }

    /**
     * 로그인 — 자격 검증 후 Access/Refresh 발급 + Refresh 화이트리스트 등록.
     *
     * <p>읽기 트랜잭션이지만 화이트리스트 등록은 DB 가 아닌 인메모리라 트랜잭션 밖 영향 없음.
     */
    @Transactional(readOnly = true)
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);

        // 사용자 부재 시에도 더미 해시로 matches 를 수행해 분기별 응답 시간 차를 줄인다(타이밍 열거 완화).
        boolean matched;
        if (user == null) {
            passwordEncoder.matches(rawPassword, DUMMY_BCRYPT_HASH);
            matched = false;
        } else {
            matched = passwordEncoder.matches(rawPassword, user.getPassword());
        }

        if (user == null || !matched) {
            log.warn("로그인 실패(자격 불일치)"); // 이메일·사유 상세 미기록(PII·열거 단서 방지)
            throw new InvalidCredentialsException();
        }

        AuthTokens tokens = issueTokens(user.getId(), user.getEmail());
        log.info("로그인 성공: userId={}", user.getId());
        return tokens;
    }

    /**
     * Access 재발급 — Refresh 검증(서명·만료·종류) + 화이트리스트 일치 확인 후 회전 발급.
     *
     * <p>토큰 회전(rotation): 재발급 시 새 Refresh 도 함께 발급하고 화이트리스트를 교체한다.
     * 탈취된 구(舊) Refresh 는 화이트리스트에서 제거되어 즉시 무효가 된다(재사용 탐지·완화).
     *
     * @param refreshToken 쿠키에서 추출한 Refresh 토큰(null 이면 누락 → 401)
     */
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }

        // 서명·만료·종류(REFRESH) 검증. 실패 시 JwtValidationException(INVALID_REFRESH_TOKEN) 발생.
        Claims claims = jwtTokenProvider.parseAndValidate(refreshToken, TokenType.REFRESH);
        Long userId = jwtTokenProvider.getUserId(claims);
        String email = jwtTokenProvider.getEmail(claims);

        // 서버 측 무효화 확인: 로그아웃/회전으로 폐기된 토큰은 화이트리스트에 없어 거부.
        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            log.warn("Refresh 화이트리스트 불일치(폐기/회전됨): userId={}", userId);
            throw new InvalidRefreshTokenException();
        }

        // 회전: 새 토큰 발급 + 화이트리스트 교체(이전 Refresh 자동 무효화).
        AuthTokens tokens = issueTokens(userId, email);
        log.info("토큰 재발급(회전) 완료: userId={}", userId);
        return tokens;
    }

    /**
     * 로그아웃 — 해당 사용자의 Refresh 화이트리스트 제거(서버 측 폐기).
     *
     * <p>Access Token 은 stateless 라 만료 전 강제 폐기가 불가하나, 만료가 짧고(15분) Refresh
     * 가 폐기되어 재발급이 막히므로 실질 세션이 종료된다.
     *
     * @param userId 인증된 사용자 ID(Access 로 식별)
     */
    public void logout(Long userId) {
        refreshTokenStore.remove(userId);
        log.info("로그아웃(Refresh 폐기): userId={}", userId);
    }

    /** Access·Refresh 동시 발급 + Refresh 화이트리스트 등록(기존 값 대체). */
    private AuthTokens issueTokens(Long userId, String email) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, email);
        refreshTokenStore.store(userId, refreshToken);
        return new AuthTokens(accessToken, refreshToken, userId, email);
    }

    /** Access 만료(초) — 컨트롤러 응답 expiresIn 용도. */
    public long accessExpiresInSeconds() {
        return jwtTokenProvider.getAccessExpiresInSeconds();
    }

    /** Refresh 만료(초) — 컨트롤러 쿠키 Max-Age 용도. */
    public long refreshExpiresInSeconds() {
        return jwtTokenProvider.getRefreshExpiresInSeconds();
    }
}
