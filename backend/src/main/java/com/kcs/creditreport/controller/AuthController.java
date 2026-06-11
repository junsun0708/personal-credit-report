package com.kcs.creditreport.controller;

import com.kcs.creditreport.dto.auth.LoginRequest;
import com.kcs.creditreport.dto.auth.LoginResponse;
import com.kcs.creditreport.dto.auth.MessageResponse;
import com.kcs.creditreport.dto.auth.SignupRequest;
import com.kcs.creditreport.dto.auth.SignupResponse;
import com.kcs.creditreport.dto.auth.TokenRefreshResponse;
import com.kcs.creditreport.security.AuthPrincipal;
import com.kcs.creditreport.security.RefreshCookieFactory;
import com.kcs.creditreport.service.AuthService;
import com.kcs.creditreport.service.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러 (API 명세서 §3.1~§3.4).
 *
 * <p>HTTP 관심사(상태코드·헤더·쿠키)만 담당하고, 비즈니스 로직은 {@link AuthService} 에 위임한다.
 * Refresh 토큰은 응답 바디에 절대 싣지 않고 HttpOnly 쿠키(Set-Cookie 헤더)로만 전달한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    /**
     * 회원가입 — 201 Created.
     *
     * <p>{@code @Valid} 로 이메일 형식·비밀번호 정책을 서버에서 강제(위반 시 400 VALIDATION_ERROR).
     * 이메일 중복은 서비스에서 409 EMAIL_DUPLICATED.
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인 — 200 OK. Access 는 바디, Refresh 는 HttpOnly 쿠키.
     *
     * <p>자격 불일치는 401 INVALID_CREDENTIALS(이메일 존재 여부 노출 안 함).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.email(), request.password());

        ResponseCookie refreshCookie = refreshCookieFactory.create(
                tokens.refreshToken(), authService.refreshExpiresInSeconds());

        LoginResponse body = LoginResponse.of(
                tokens.accessToken(),
                authService.accessExpiresInSeconds(),
                tokens.userId(),
                tokens.email());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    /**
     * Access 재발급 — 200 OK. Refresh 쿠키 검증 후 새 Access + 회전된 Refresh 쿠키.
     *
     * <p>쿠키 누락·만료·위변조·폐기는 401 INVALID_REFRESH_TOKEN(서비스/필터에서 처리).
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(HttpServletRequest httpRequest) {
        String refreshToken = refreshCookieFactory.resolve(httpRequest);
        AuthTokens tokens = authService.refresh(refreshToken);

        // 토큰 회전: 새 Refresh 쿠키를 다시 내려준다(명세 §3.3 선택사항을 보안상 항상 적용).
        ResponseCookie rotatedCookie = refreshCookieFactory.create(
                tokens.refreshToken(), authService.refreshExpiresInSeconds());

        TokenRefreshResponse body = TokenRefreshResponse.of(
                tokens.accessToken(), authService.accessExpiresInSeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rotatedCookie.toString())
                .body(body);
    }

    /**
     * 로그아웃 — 200 OK. Refresh 화이트리스트 폐기 + 쿠키 삭제.
     *
     * <p>Access 인증 필요(필터가 SecurityContext 설정). 미인증 시 401 UNAUTHORIZED(EntryPoint).
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal AuthPrincipal principal) {

        authService.logout(principal.userId());

        ResponseCookie deletedCookie = refreshCookieFactory.delete();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deletedCookie.toString())
                .body(MessageResponse.of("로그아웃되었습니다."));
    }
}
