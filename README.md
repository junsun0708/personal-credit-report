# 개인 신용평가 리포트 조회 서비스

한국평가정보(KCS) Product Engineer 사전 과제입니다.
회원가입·로그인 후 본인의 신용평가 리포트를 조회하고, 상세 확인 및 조회 이력을 관리하는 풀스택 웹 애플리케이션입니다.

---

## 기술 스택

### Frontend
- **Next.js (App Router)** + **TypeScript**
- **Zustand** — 클라이언트 상태(인증 토큰·유저)
- **TanStack Query** — 서버 상태·캐싱
- **react-hook-form + zod** — 폼·검증
- **Tailwind CSS** — 스타일링

### Backend
- **Spring Boot 3.4** / **Java 17**
- **Spring Security** + **JWT(JJWT)** — 인증
- **Spring Data JPA(Hibernate)**
- **H2 (인메모리)** — DB

> Backend는 가산점 옵션인 **Spring Boot(Option A)**로 구현했습니다. 실제 협업 환경(Java + Spring)을 재현하고, Spring Security 필터 체인으로 인증 흐름을 명시적으로 설계하기 위함입니다.

---

## 프로젝트 구조

```
personal-credit-report/
├── frontend/          # Next.js 앱
│   ├── app/           # 라우트((auth)/(protected) 그룹)
│   ├── components/    # ui(공통) / domain(도메인) / layout
│   ├── lib/           # api 클라이언트, query, validators, utils
│   ├── stores/        # Zustand 인증 스토어
│   ├── hooks/         # 데이터 훅·URL 동기화
│   └── types/         # 도메인 타입
├── backend/           # Spring Boot 앱
│   └── src/main/java/com/kcs/creditreport/
│       ├── domain/        # 엔티티 (User, CreditReport, ViewHistory)
│       ├── repository/    # JPA Repository (+ Specification)
│       ├── service/       # 비즈니스 로직
│       ├── controller/    # REST 컨트롤러
│       ├── security/      # JWT 발급·필터·SecurityConfig
│       ├── dto/ · exception/ · config/ · util/
│       └── resources/     # application.yml, data.sql(시드)
├── docs/              # 설계 문서 9종 (요구사항·아키텍처·DB·API·화면·테스트·일정)
└── README.md
```

---

## 실행 방법

### 사전 요구
- **JDK 17 이상** (백엔드 빌드/실행 — `javac` 포함된 JDK 필요)
- **Node.js 18 이상**

### 한 번에 실행 (백엔드 + 프론트 동시) — 권장

프로젝트 루트에서:

```bash
./dev.sh
```

- 백엔드(`:8080`)와 프론트(`:3000`)를 **동시에 기동**합니다. (최초 실행 시 프론트 의존성을 자동 설치)
- 종료는 `Ctrl+C` 한 번이면 둘 다 정리됩니다.
- 브라우저에서 `http://localhost:3000` 접속 → `test@kcs.com` / `Test1234!` 로 로그인.

> 개별 실행을 원하면 아래를 참고하세요.

### 1) Backend

```bash
cd backend
COOKIE_SECURE=false ./gradlew bootRun
```

- `http://localhost:8080` 에서 기동됩니다.
- **`COOKIE_SECURE=false`** 는 로컬 `http` 환경에서 Refresh 쿠키(기본 Secure)가 브라우저로 전송되도록 하는 설정입니다. (운영 HTTPS에서는 생략 → Secure 쿠키)
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:creditdb`)

### 2) Frontend

```bash
cd frontend
npm install
npm run dev
```

- `http://localhost:3000` 에서 기동됩니다.
- API 주소는 `NEXT_PUBLIC_API_BASE_URL`(미설정 시 `http://localhost:8080`). 필요 시 `.env.local.example`을 복사해 사용하세요.

### 테스트 계정

| 이메일 | 비밀번호 | 비고 |
|---|---|---|
| `test@kcs.com` | `Test1234!` | 신용리포트 28건 |
| `jyh@kcs.com` | `Test1234!` | 신용리포트 16건 |

> 앱 기동 시 위 두 계정과 신용리포트(test 28건 / jyh 16건)가 시드로 자동 주입됩니다(등급 1~10·발급일·기관 분산).
> 신규로 회원가입한 계정은 본인 리포트가 없는 게 정상입니다(본인 데이터만 보이는 권한격리). 데이터 확인은 위 시드 계정으로 로그인하세요.

---

## API 명세

| Method | Endpoint | 설명 | 인증 |
|---|---|---|:--:|
| POST | `/api/auth/signup` | 회원가입(이메일·비번정책 검증) | ✕ |
| POST | `/api/auth/login` | 로그인 → Access(body) + Refresh(HttpOnly 쿠키) | ✕ |
| POST | `/api/auth/refresh` | Refresh 쿠키로 Access 재발급(회전) | 쿠키 |
| POST | `/api/auth/logout` | 로그아웃(Refresh 무효화) | ✓ |
| GET | `/api/reports` | 목록(page,size=10,q,grade,from,to,sort,order) | ✓ |
| GET | `/api/reports/{id}` | 상세(주민번호 마스킹) + 조회이력 자동기록 | ✓ |
| GET | `/api/histories` | 조회 이력(최신순) | ✓ |

- 공통 페이지 응답: `{ data, page, size, totalElements, totalPages }`
- 공통 에러 응답: `{ code, message }`
- 페이지네이션·검색·필터·정렬은 **전부 서버에서 처리**(JPA `Pageable` + Specification 동적쿼리).
- 상세 설계: [`docs/05_API_명세서.md`](./docs/05_API_명세서.md)

---

## 주요 설계 결정 / 트레이드오프

- **토큰 전략 — Access(메모리) / Refresh(HttpOnly 쿠키)**: Access는 클라이언트 메모리(Zustand)에만 둬 XSS 노출면을 줄이고, Refresh는 JS 접근 불가 쿠키(`HttpOnly`+`SameSite=Strict`)로 보관해 XSS/CSRF를 동시에 방어. 대신 새로고침 시 Access가 사라지므로 **앱 마운트 시 silent refresh**로 복원(초기 깜빡임은 가드 처리).
- **자동 재발급 — single-flight**: 401 발생 시 refresh를 단일 Promise로 처리해 동시 요청이 한 번만 재발급하도록 큐잉, 실패 시 로그아웃.
- **권한 격리**: 모든 도메인 쿼리에 `user_id` 스코프를 강제(`findByIdAndUserId`, Specification). 타인 리포트 ID 직접 요청 시 **404로 존재 자체를 은닉**(IDOR 방지).
- **민감정보 마스킹**: 주민번호는 응답 DTO 생성 시점에 `900101-1******`로 마스킹 — 평문이 네트워크/로그로 나가지 않음.
- **사용자 열거 방지**: 로그인 실패는 이메일 부재/비번 불일치를 동일 메시지로 통일.
- **H2 인메모리**: 5분 내 기동·실행 편의성 우선. 재기동 시 `data.sql`로 시드 재주입해 데모 재현성 확보.

---

## 테스트

```bash
cd backend
./gradlew test
```

- 인증 흐름·권한 격리·마스킹·이력 기록·페이징/정렬/필터를 다루는 **통합 테스트(MockMvc) + 단위 테스트 58개**.
- 실제 기동 후 `로그인 → 목록 → 상세(마스킹) → 이력 기록 → 미인증 401` end-to-end 동작 확인 완료.

---

## 설계 문서

`docs/` 폴더에 기획→요구사항→설계→테스트→일정 흐름의 문서 9종이 있습니다. 시작점은 [`docs/00_INDEX.md`](./docs/00_INDEX.md).
