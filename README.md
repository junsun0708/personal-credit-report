# 개인 신용평가 리포트 조회 서비스

한국평가정보(KCS) Product Engineer 사전 과제입니다.
회원가입·로그인 후 본인의 신용평가 리포트를 조회하고, 조회 이력을 관리하는 풀스택 웹 애플리케이션입니다.

## 기술 스택

- **Frontend**: Next.js 14 (App Router), TypeScript, Zustand, TanStack Query, Tailwind CSS, shadcn/ui
- **Backend**: Spring Boot 3.x, Spring Security, JPA(Hibernate), H2 (인메모리)
- **인증**: JWT (Access / Refresh), BCrypt

> 백엔드는 가산점 옵션인 Spring Boot(Option A)로 구현합니다.

## 프로젝트 구조

```
personal-credit-report/
├── frontend/   # Next.js 14 앱
├── backend/    # Spring Boot 앱
├── docs/       # 설계 문서 (요구사항·아키텍처·DB·API·화면·테스트·일정)
└── README.md
```

## 실행 방법

> 구현 진행하며 채워나갈 예정입니다. (FE / BE 각각 5분 내 기동 목표)

### Backend

```bash
cd backend
./gradlew bootRun
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 테스트 계정

> 시드 데이터 작성 후 기재 예정.

## API 명세

> 상세는 [docs/05_API_명세서.md](./docs/05_API_명세서.md) 참고. (Swagger UI 링크는 구현 후 기재)

## 구현하며 고민한 점 / 트레이드오프

> 작업 진행하며 정리.

## 시간 부족으로 구현하지 못한 부분

> 마무리 단계에서 정리.
