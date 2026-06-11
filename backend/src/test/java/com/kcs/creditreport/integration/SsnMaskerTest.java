package com.kcs.creditreport.integration;

import com.kcs.creditreport.util.SsnMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-11/12/13 SsnMasker 단위 테스트
 *
 * <p>마스킹은 서버 응답 DTO 생성 시점에 적용되어 네트워크로 평문이 전달되지 않아야 한다.
 * 이 테스트는 유틸 로직 자체의 정확성을 검증한다 (설계서 §6.2).
 * <ul>
 *   <li>SEC-13 : 다양한 입력 패턴에 대한 정확한 마스킹</li>
 *   <li>정상 SSN → 뒤 6자리 별표 치환</li>
 *   <li>null / 짧은 입력 → 안전 기본값 반환</li>
 * </ul>
 */
@DisplayName("SsnMasker 단위 테스트 (SEC-11/12/13)")
class SsnMaskerTest {

    // ── 정상 입력 마스킹 ──────────────────────────────────────────────

    @Test
    @DisplayName("정상 주민번호 '900101-1234567' 이 '900101-1******' 으로 마스킹된다")
    void mask_normalSsn_masksLast6Digits() {
        String result = SsnMasker.mask("900101-1234567");
        assertThat(result).isEqualTo("900101-1******");
    }

    @Test
    @DisplayName("앞 8자(YYMMDD-G)만 노출하고 뒤 6자리는 '*' 6개로 치환된다")
    void mask_exposes8CharsAndMasksRemainder() {
        // YYMMDD-G + 뒤 6자리 = 14자 총 길이
        String input = "880520-1234567";
        String result = SsnMasker.mask(input);

        // 앞 8자는 원본 그대로
        assertThat(result.substring(0, 8)).isEqualTo("880520-1");
        // 뒤는 정확히 '******'
        assertThat(result.substring(8)).isEqualTo("******");
        // 총 길이: 8 + 6 = 14
        assertThat(result).hasSize(14);
    }

    @Test
    @DisplayName("여성 주민번호(성별 코드 2) 도 동일하게 마스킹된다")
    void mask_femaleSsn_masksCorrectly() {
        String result = SsnMasker.mask("930415-2234567");
        assertThat(result).isEqualTo("930415-2******");
    }

    // ── 비정상 입력 안전 처리 ─────────────────────────────────────────

    @Test
    @DisplayName("null 입력 시 안전 기본값('******')을 반환한다")
    void mask_nullInput_returnsFallback() {
        String result = SsnMasker.mask(null);
        assertThat(result).isEqualTo("******");
    }

    @ParameterizedTest(name = "짧은 입력 '{0}' 은 안전 기본값을 반환한다")
    @ValueSource(strings = {"", "900101", "900101-"})
    @DisplayName("길이 미달 입력 시 안전 기본값을 반환한다")
    void mask_tooShortInput_returnsFallback(String input) {
        String result = SsnMasker.mask(input);
        assertThat(result).isEqualTo("******");
    }

    // ── 마스킹 결과 패턴 검증 ─────────────────────────────────────────

    @Test
    @DisplayName("마스킹 결과가 정규식 패턴 '\\d{6}-\\d\\*{6}' 과 일치한다")
    void mask_result_matchesExpectedPattern() {
        // API 응답 검증과 동일한 정규식 사용
        String result = SsnMasker.mask("870311-1234567");
        assertThat(result).matches("\\d{6}-\\d\\*{6}");
    }

    @Test
    @DisplayName("마스킹 결과에 원본 뒤 6자리 숫자가 포함되지 않는다(평문 미전송)")
    void mask_result_doesNotContainOriginalLastDigits() {
        String original = "910722-1999888";
        String result = SsnMasker.mask(original);

        // 뒤 6자리 '999888' 이 결과에 노출되지 않아야 한다
        assertThat(result).doesNotContain("999888");
        // 대신 '******' 이 포함된다
        assertThat(result).contains("******");
    }
}
