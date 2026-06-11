package com.kcs.creditreport.repository;

import com.kcs.creditreport.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 Repository.
 *
 * <p>로그인 조회({@link #findByEmail})와 회원가입 중복 검증({@link #existsByEmail})을 제공한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 이메일로 회원 조회 (로그인 인증). */
    Optional<User> findByEmail(String email);

    /** 이메일 존재 여부 (회원가입 중복 차단). */
    boolean existsByEmail(String email);
}
