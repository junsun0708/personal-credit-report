package com.kcs.creditreport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회원 엔티티.
 *
 * <p>H2 예약어 {@code user} 충돌을 회피하기 위해 물리 테이블명을 {@code users} 로 매핑한다.
 * (설계서 §1.2, §3.1 참조)
 *
 * <p>{@code password} 는 절대 평문 저장하지 않고 BCrypt 단방향 해시(60자 고정)를 저장한다.
 * API 응답 DTO 에는 포함하지 않는다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션 전용 기본 생성자
public class User {

    /** 회원 식별자 (PK, AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 ID (이메일 형식). UNIQUE 제약으로 중복 가입 차단. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt 해시 비밀번호 (60자 고정). 평문 저장 금지. */
    @Column(nullable = false, length = 60)
    private String password;

    /** 가입 일시. 영속화 시점에 자동 기록. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * 회원 가입용 정적 팩토리.
     *
     * @param email          이메일(로그인 ID)
     * @param encodedPassword 이미 BCrypt 로 인코딩된 비밀번호 해시
     */
    public static User of(String email, String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .build();
    }
}
