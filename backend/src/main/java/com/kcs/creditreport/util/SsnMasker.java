package com.kcs.creditreport.util;

/**
 * 주민등록번호 마스킹 유틸 (설계서 §6.2).
 *
 * <p>마스킹 규칙: 생년월일 6자리 + 성별 1자리만 노출하고, 뒤 6자리를 {@code *} 로 치환한다.
 * <pre>{@code "900101-1234567" -> "900101-1******"}</pre>
 *
 * <p>마스킹은 <b>서버 응답 DTO 생성 시점</b>에 수행해 평문이 네트워크로 절대 노출되지 않게 한다.
 * (클라이언트 마스킹 금지 — 원본이 전송되면 무의미)
 *
 * <p>보안: 입력 원본(평문 ssn)을 로그/예외 메시지에 출력하지 않는다.
 */
public final class SsnMasker {

    /** 노출 길이: {@code YYMMDD-G} (앞 6자리 + 하이픈 + 성별 1자리) = 8자. */
    private static final int VISIBLE_PREFIX_LENGTH = 8;

    /** 뒤 6자리 마스킹 문자열. */
    private static final String MASK = "******";

    /** 형식 미상/누락 시 안전 기본값(원본 추정 불가하도록 전부 마스킹). */
    private static final String FALLBACK = "******";

    private SsnMasker() {
        // 유틸 클래스 — 인스턴스화 금지
    }

    /**
     * 주민등록번호를 {@code 900101-1******} 형식으로 마스킹한다.
     *
     * <p>{@code "YYMMDD-XXXXXXX"} (14자) 정상 입력은 앞 8자({@code YYMMDD-G})를 노출하고
     * 뒤 6자리를 마스킹한다. null·길이 미달 등 비정상 입력은 전부 마스킹한 안전값을 반환한다.
     *
     * @param ssn 평문 주민등록번호(예: {@code "900101-1234567"})
     * @return 마스킹된 문자열(예: {@code "900101-1******"}), 비정상 입력 시 {@code "******"}
     */
    public static String mask(String ssn) {
        if (ssn == null || ssn.length() < VISIBLE_PREFIX_LENGTH) {
            return FALLBACK;
        }
        return ssn.substring(0, VISIBLE_PREFIX_LENGTH) + MASK;
    }
}
