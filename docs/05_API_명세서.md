# API 명세서

| 항목 | 내용 |
|---|---|
| 문서번호 | 05 |
| 버전 | v1.0 |
| 작성일 | 2026-06-11 |
| 상태 | 초안 |

> 개인 신용평가 리포트 조회 서비스 — 한국평가정보(KCS) Product Engineer 사전 과제
> 백엔드: Spring Boot 3.x / Spring Security 6 / JPA / H2
> 데이터 모델(엔티티·컬럼·제약)은 [`04_데이터베이스_설계서.md`](./04_데이터베이스_설계서.md) 를 참조한다.

---

## 1. 공통 규약

### 1.1 Base URL

| 환경 | Base URL |
|---|---|
| 로컬(개발) | `http://localhost:8080` |
| API 프리픽스 | 모든 엔드포인트는 `/api` 하위에 존재 |

예) 리포트 목록 = `http://localhost:8080/api/reports`

### 1.2 인증 헤더

인증이 필요한 모든 요청은 **Access Token**을 `Authorization` 헤더에 Bearer 스킴으로 담아 전송한다.

```
Authorization: Bearer <accessToken>
```

- Access Token은 클라이언트 **메모리(Zustand)** 에 보관한다(localStorage 미사용 — XSS 노출면 최소화).
- Refresh Token은 서버가 내려주는 **HttpOnly + Secure + SameSite 쿠키**로 관리되며, 클라이언트 JS가 직접 다루지 않는다.
- 자세한 내용은 [4. 인증/토큰 규약](#4-인증토큰-규약) 참조.

### 1.3 공통 요청 규약

| 항목 | 값 |
|---|---|
| Content-Type (요청 바디 존재 시) | `application/json` |
| Accept | `application/json` |
| 문자 인코딩 | UTF-8 |
| 날짜 형식 | `yyyy-MM-dd` (쿼리), `yyyy-MM-dd'T'HH:mm:ss` (응답 timestamp, ISO-8601) |

### 1.4 공통 응답 포맷 — 페이지네이션

목록을 반환하는 모든 엔드포인트는 아래 페이지 래퍼 포맷을 따른다.

```json
{
  "data": [],
  "page": 1,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `data` | array | 현재 페이지의 항목 배열 |
| `page` | number | 현재 페이지 번호 (1-based) |
| `size` | number | 페이지당 항목 수 |
| `totalElements` | number | 필터 적용 후 전체 항목 수 |
| `totalPages` | number | 전체 페이지 수 |

> 단건 조회(예: 리포트 상세)는 래퍼 없이 객체를 직접 반환한다.

### 1.5 공통 에러 포맷

모든 에러 응답은 아래 포맷으로 통일한다. (`@RestControllerAdvice` 전역 처리)

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `code` | string | 기계 판독용 에러 코드(UPPER_SNAKE_CASE) |
| `message` | string | 사용자/개발자용 한국어 설명 |

#### 공통 에러 코드 목록

| code | HTTP | 의미 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 요청 값 검증 실패(형식·정책 위반) |
| `INVALID_CREDENTIALS` | 401 | 인증 정보 불일치 |
| `UNAUTHORIZED` | 401 | Access Token 누락·만료·위변조 |
| `TOKEN_EXPIRED` | 401 | Access Token 만료(클라이언트 refresh 트리거) |
| `INVALID_REFRESH_TOKEN` | 401 | Refresh Token 누락·만료·폐기됨 |
| `FORBIDDEN` | 403 | 타인 리소스 접근 등 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `EMAIL_DUPLICATED` | 409 | 이미 가입된 이메일 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### 1.6 HTTP 상태코드 정책

| 상태코드 | 사용 상황 |
|---|---|
| `200 OK` | 조회·로그인·재발급·로그아웃 성공 |
| `201 Created` | 회원가입 성공(리소스 생성) |
| `400 Bad Request` | 입력 검증 실패 |
| `401 Unauthorized` | 인증 실패·토큰 만료 |
| `403 Forbidden` | 인증은 되었으나 권한 없음 |
| `404 Not Found` | 리소스 없음(타인 리포트 ID 포함) |
| `409 Conflict` | 이메일 중복 |
| `500 Internal Server Error` | 서버 내부 오류 |

> 권한 격리 정책: 타인의 리포트 ID를 직접 요청해도 정보 노출을 막기 위해 `404 Not Found`를 우선 반환한다(존재 여부 자체를 숨김).

---

## 2. 엔드포인트 요약

| # | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | 회원가입 | ✕ |
| 2 | POST | `/api/auth/login` | 로그인 → Access(body) + Refresh(쿠키) | ✕ |
| 3 | POST | `/api/auth/refresh` | Refresh 쿠키로 Access 재발급 | 쿠키 |
| 4 | POST | `/api/auth/logout` | 로그아웃(Refresh 무효화) | ✓ |
| 5 | GET | `/api/reports` | 리포트 목록(페이징·검색·필터·정렬) | ✓ |
| 6 | GET | `/api/reports/{id}` | 리포트 상세(조회 이력 자동 기록) | ✓ |
| 7 | GET | `/api/histories` | 내 조회 이력(최신순·페이징) | ✓ |

> 인증 표기: `✕`=불필요, `✓`=Access Token(Bearer) 필요, `쿠키`=Refresh 쿠키로 인증.

---

## 3. 엔드포인트 상세

### 3.1 POST /api/auth/signup — 회원가입

이메일·비밀번호 기반 회원가입. 비밀번호는 서버에서 BCrypt로 해시 저장한다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Content-Type` | ✓ | `application/json` |

**요청 바디**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `email` | string | ✓ | 이메일 형식, 미가입 이메일 |
| `password` | string | ✓ | 8자 이상, 영문/숫자/특수문자 조합 |

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!"
}
```

**성공 응답 — `201 Created`**

```json
{
  "id": 1,
  "email": "user@example.com",
  "createdAt": "2026-06-11T10:00:00"
}
```

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| 이메일 형식 오류 / 비밀번호 정책 위반 | 400 | `VALIDATION_ERROR` |
| 이미 가입된 이메일 | 409 | `EMAIL_DUPLICATED` |

---

### 3.2 POST /api/auth/login — 로그인

자격 증명 검증 후 Access Token(바디) + Refresh Token(HttpOnly 쿠키)을 발급한다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Content-Type` | ✓ | `application/json` |

**요청 바디**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `email` | string | ✓ | 이메일 형식 |
| `password` | string | ✓ | — |

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!"
}
```

**성공 응답 — `200 OK`**

응답 헤더에 Refresh 쿠키가 설정된다.

```
Set-Cookie: refreshToken=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "email": "user@example.com"
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `accessToken` | string | Access Token(JWT) |
| `tokenType` | string | 고정값 `Bearer` |
| `expiresIn` | number | Access Token 만료(초). 예: 900(15분) |
| `user` | object | 로그인 사용자 요약 |

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| 입력 형식 오류 | 400 | `VALIDATION_ERROR` |
| 이메일/비밀번호 불일치 | 401 | `INVALID_CREDENTIALS` |

---

### 3.3 POST /api/auth/refresh — Access 재발급

Refresh 쿠키를 검증하여 새 Access Token을 발급한다. 요청 바디는 없으며, 쿠키가 자동 전송된다.

**요청**

| 항목 | 내용 |
|---|---|
| 헤더 | Authorization 불필요 |
| 쿠키 | `refreshToken=<jwt>` (HttpOnly, 브라우저 자동 전송) |
| 바디 | 없음 |

**성공 응답 — `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...newtoken...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

> 토큰 로테이션을 적용할 경우, 응답 헤더로 새 Refresh 쿠키를 다시 내려준다(선택).

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| Refresh 쿠키 누락 | 401 | `INVALID_REFRESH_TOKEN` |
| Refresh 만료·위변조·폐기됨 | 401 | `INVALID_REFRESH_TOKEN` |

---

### 3.4 POST /api/auth/logout — 로그아웃

Refresh Token을 서버에서 무효화하고 쿠키를 삭제한다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Authorization` | ✓ | `Bearer <accessToken>` |

쿠키 `refreshToken`도 함께 전송된다(서버 폐기 대상 식별).

**성공 응답 — `200 OK`**

응답 헤더에서 Refresh 쿠키를 만료시킨다.

```
Set-Cookie: refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0
```

```json
{
  "message": "로그아웃되었습니다."
}
```

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| Access Token 누락·만료 | 401 | `UNAUTHORIZED` |

---

### 3.5 GET /api/reports — 리포트 목록 조회

본인 소유의 신용평가 리포트 목록을 **서버 측 페이지네이션·검색·필터·정렬**로 반환한다. 모든 처리는 서버에서 수행하며(`Pageable` + JPA Specification 동적 쿼리), 클라이언트 필터링은 사용하지 않는다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Authorization` | ✓ | `Bearer <accessToken>` |

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | number | ✕ | `1` | 페이지 번호(1-based) |
| `size` | number | ✕ | `10` | 페이지당 항목 수(과제 기준 10 고정 권장) |
| `q` | string | ✕ | — | 검색어 — 리포트 제목(`title`) 또는 발급기관명(`institution`) 부분 일치 |
| `grade` | number | ✕ | — | 신용등급 필터(1~10) |
| `from` | string | ✕ | — | 발급일 시작(`yyyy-MM-dd`, 포함) |
| `to` | string | ✕ | — | 발급일 종료(`yyyy-MM-dd`, 포함) |
| `sort` | string | ✕ | `issuedDate` | 정렬 기준 — `issuedDate`(발급일) \| `creditScore`(신용점수) |
| `order` | string | ✕ | `desc` | 정렬 방향 — `asc` \| `desc` |

> 검색·필터·정렬 상태는 프론트엔드에서 URL 쿼리와 동기화되어 새로고침 시 유지된다. 본 명세는 서버 계약만 정의한다.

**요청 예시**

```
GET /api/reports?page=1&size=10&q=신한&grade=2&from=2025-01-01&to=2025-12-31&sort=creditScore&order=desc
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**성공 응답 — `200 OK`**

```json
{
  "data": [
    {
      "id": 12,
      "title": "2025년 상반기 신용평가 리포트",
      "institution": "신한카드",
      "creditScore": 842,
      "creditGrade": 2,
      "issuedDate": "2025-06-30"
    },
    {
      "id": 9,
      "title": "정기 신용 리포트",
      "institution": "신한은행",
      "creditScore": 815,
      "creditGrade": 2,
      "issuedDate": "2025-03-15"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1
}
```

| 항목 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 리포트 ID |
| `title` | string | 리포트 제목 |
| `institution` | string | 발급기관명 |
| `creditScore` | number | 신용점수 |
| `creditGrade` | number | 신용등급(1~10) |
| `issuedDate` | string | 발급일(`yyyy-MM-dd`) |

> 목록 응답에는 민감정보(주민번호 등)를 포함하지 않는다. 상세 항목은 [3.6 상세 조회](#36-get-apireportsid--리포트-상세-조회) 참조.

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| Access Token 누락·만료 | 401 | `UNAUTHORIZED` |
| `grade` 범위 외 / 날짜 형식 오류 / `sort`·`order` 미허용 값 | 400 | `VALIDATION_ERROR` |

---

### 3.6 GET /api/reports/{id} — 리포트 상세 조회

리포트 단건의 상세 정보를 반환한다. **상세 진입 시 조회 이력(ViewHistory)이 자동으로 1건 INSERT** 된다(중복 조회 시에도 각 시점을 모두 기록). 민감정보는 **서버 응답 단계에서 마스킹**되어 평문이 네트워크로 노출되지 않는다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Authorization` | ✓ | `Bearer <accessToken>` |

**경로 변수**

| 변수 | 타입 | 설명 |
|---|---|---|
| `id` | number | 조회할 리포트 ID |

**요청 예시**

```
GET /api/reports/12
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**성공 응답 — `200 OK`**

```json
{
  "id": 12,
  "title": "2025년 상반기 신용평가 리포트",
  "institution": "신한카드",
  "creditScore": 842,
  "creditGrade": 2,
  "issuedDate": "2025-06-30",
  "maskedSsn": "900101-1******",
  "holderName": "홍길동",
  "createdAt": "2025-06-30T09:00:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 리포트 ID |
| `title` | string | 리포트 제목 |
| `institution` | string | 발급기관명 |
| `creditScore` | number | 신용점수(게이지 시각화 대상) |
| `creditGrade` | number | 신용등급(1~10) |
| `issuedDate` | string | 발급일(`yyyy-MM-dd`) |
| `maskedSsn` | string | 마스킹된 주민등록번호(예: `900101-1******`) |
| `holderName` | string | 명의자명 |
| `createdAt` | string | 리포트 생성 시각(ISO-8601) |

> 부수효과: 본 요청 성공 시 `viewed_at = 현재시각`으로 ViewHistory가 기록되며, 이 변경으로 클라이언트는 `['histories']` 캐시를 invalidate한다.

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| Access Token 누락·만료 | 401 | `UNAUTHORIZED` |
| 리포트 없음 / **타인 소유 리포트 ID** | 404 | `NOT_FOUND` |

> 권한 격리: 모든 조회 쿼리에 `WHERE user_id = :currentUserId`를 강제하므로, 타인 리포트 ID 요청은 존재 여부를 숨기기 위해 `404`로 응답한다.

---

### 3.7 GET /api/histories — 조회 이력 목록

본인이 조회한 리포트 이력을 **최신순(`viewed_at DESC`)** 으로 페이지네이션하여 반환한다. 동일 리포트를 여러 번 조회한 경우 각 시점이 모두 별도 항목으로 나타난다.

**요청 헤더**

| 헤더 | 필수 | 값 |
|---|---|---|
| `Authorization` | ✓ | `Bearer <accessToken>` |

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | number | ✕ | `1` | 페이지 번호(1-based) |
| `size` | number | ✕ | `10` | 페이지당 항목 수 |

> 정렬은 항상 최신순 고정이므로 `sort`/`order` 파라미터를 받지 않는다.

**요청 예시**

```
GET /api/histories?page=1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**성공 응답 — `200 OK`**

```json
{
  "data": [
    {
      "id": 101,
      "reportId": 12,
      "reportTitle": "2025년 상반기 신용평가 리포트",
      "institution": "신한카드",
      "creditGrade": 2,
      "viewedAt": "2026-06-11T10:32:11"
    },
    {
      "id": 100,
      "reportId": 12,
      "reportTitle": "2025년 상반기 신용평가 리포트",
      "institution": "신한카드",
      "creditGrade": 2,
      "viewedAt": "2026-06-11T09:58:04"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1
}
```

| 항목 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 이력 ID |
| `reportId` | number | 조회한 리포트 ID(상세 페이지 링크용) |
| `reportTitle` | string | 리포트 제목 |
| `institution` | string | 발급기관명 |
| `creditGrade` | number | 신용등급(1~10) |
| `viewedAt` | string | 조회 시각(ISO-8601) |

**에러 케이스**

| 상황 | HTTP | code |
|---|---|---|
| Access Token 누락·만료 | 401 | `UNAUTHORIZED` |
| `page`/`size` 형식 오류 | 400 | `VALIDATION_ERROR` |

---

## 4. 인증/토큰 규약

### 4.1 토큰 구조

| 토큰 | 만료 | 저장 위치 | 전송 방식 | 목적 |
|---|---|---|---|---|
| Access Token | 15분(`expiresIn=900`) | 클라이언트 **메모리(Zustand)** | `Authorization: Bearer` 헤더 | API 요청 인증 |
| Refresh Token | 7일 | **HttpOnly + Secure + SameSite 쿠키** | 쿠키 자동 전송 | Access 재발급 |

- Access Token을 localStorage에 두지 않아 **XSS 노출면을 최소화**한다.
- Refresh Token은 JS로 접근 불가한 쿠키이므로 탈취 위험을 낮춘다(XSS 방어).
- Refresh 쿠키는 `Path=/api/auth`로 범위를 제한하여 일반 API 요청에는 동봉되지 않게 한다.

### 4.2 쿠키 속성

| 속성 | 값 | 의미 |
|---|---|---|
| `HttpOnly` | true | JS 접근 차단(XSS 방어) |
| `Secure` | true | HTTPS 전송 한정 |
| `SameSite` | `Strict` | CSRF 표면 축소 |
| `Path` | `/api/auth` | 인증 엔드포인트로 범위 제한 |
| `Max-Age` | `604800` | 7일(초) |

### 4.3 401 → refresh 자동 재발급 흐름

```
1. 클라이언트가 보호 API 호출 (Authorization: Bearer <access>)
2. Access 만료 → 서버 401 (code: UNAUTHORIZED / TOKEN_EXPIRED)
3. Axios 응답 인터셉터가 401 감지
4. POST /api/auth/refresh 호출 (Refresh 쿠키 자동 전송, 바디 없음)
   ├─ 성공(200) → 새 accessToken 메모리 갱신 → 원래 요청 재시도
   └─ 실패(401, INVALID_REFRESH_TOKEN) → 로그아웃 처리 → /login 리다이렉트
5. 동시 다발 401 발생 시 refresh 요청을 단일화(큐잉)하여 중복 호출 방지
```

> 서버 부트스트랩(새로고침)으로 Access가 메모리에서 소실되면, 앱 마운트 시 `/api/auth/refresh`로 silent 재발급을 시도한다.

---

## 5. 페이지네이션·검색·필터·정렬 파라미터 규약

> 본 절의 규약은 **서버 측 처리 원칙**(과제 필수)에 따라 모두 백엔드에서 적용된다. 클라이언트 단순 필터링은 사용하지 않는다.

### 5.1 페이지네이션

| 파라미터 | 기본 | 규약 |
|---|---|---|
| `page` | 1 | 1-based. 범위 초과 시 빈 `data`와 함께 메타 반환 |
| `size` | 10 | 리포트 목록은 페이지당 10건(과제 기준). 상한 권장 100 |

응답 메타: `page`, `size`, `totalElements`, `totalPages` ([1.4](#14-공통-응답-포맷--페이지네이션) 참조).

### 5.2 검색 (`q`) — `/api/reports` 한정

| 항목 | 규약 |
|---|---|
| 대상 컬럼 | `title` OR `institution` |
| 매칭 방식 | 대소문자 무시 부분 일치(`LIKE %q%`) |
| 공백/미지정 | 미적용(전체 대상) |

### 5.3 필터 — `/api/reports` 한정

| 파라미터 | 규약 |
|---|---|
| `grade` | 정확 일치(1~10). 범위 외 → `400 VALIDATION_ERROR` |
| `from` | `issuedDate >= from` (포함). `yyyy-MM-dd` |
| `to` | `issuedDate <= to` (포함). `yyyy-MM-dd` |
| `from` > `to` | `400 VALIDATION_ERROR` |

복수 필터는 AND로 결합한다(예: `grade=2 AND from<=issuedDate<=to`).

### 5.4 정렬 — `/api/reports` 한정

| 파라미터 | 허용 값 | 기본 | 규약 |
|---|---|---|---|
| `sort` | `issuedDate`, `creditScore` | `issuedDate` | 미허용 값 → `400 VALIDATION_ERROR` |
| `order` | `asc`, `desc` | `desc` | 미허용 값 → `400 VALIDATION_ERROR` |

> `/api/histories`는 최신순(`viewedAt DESC`) 고정으로 `sort`/`order`를 받지 않는다.

### 5.5 파라미터 적용 순서

서버는 다음 순서로 쿼리를 구성한다.

```
[권한 스코프] WHERE user_id = :currentUserId
  → [검색]   AND (title LIKE %q% OR institution LIKE %q%)
  → [필터]   AND grade = :grade AND issuedDate BETWEEN :from AND :to
  → [정렬]   ORDER BY :sort :order
  → [페이징] LIMIT :size OFFSET (page-1)*size
```

---

> 본 문서는 백엔드 API 계약의 기준선이다. 데이터 모델 상세는 [`04_데이터베이스_설계서.md`](./04_데이터베이스_설계서.md), 인증·보안 설계 배경은 개발계획서 §5를 참조한다.
