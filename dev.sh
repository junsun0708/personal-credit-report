#!/usr/bin/env bash
#
# 백엔드(Spring Boot, 8080) + 프론트(Next.js, 3000) 동시 기동 스크립트
# 사용법: 프로젝트 루트에서  ./dev.sh
# 종료:   Ctrl+C  (두 프로세스 함께 정리)
#
set -e
cd "$(dirname "$0")"

# 최초 1회: 프론트 의존성 설치
if [ ! -d frontend/node_modules ]; then
  echo "▶ 프론트엔드 의존성 설치 중..."
  (cd frontend && npm install)
fi

# Ctrl+C 등 종료 시 자식 프로세스(백엔드·프론트) 모두 정리
trap 'echo; echo "▶ 종료 중..."; kill 0' EXIT INT TERM

echo "▶ 백엔드(http://localhost:8080) + 프론트(http://localhost:3000) 기동..."
echo "  (백엔드 최초 빌드는 시간이 조금 걸릴 수 있습니다)"
echo

# 백엔드: 로컬 http에서 Refresh 쿠키 전송 위해 COOKIE_SECURE=false
(cd backend && COOKIE_SECURE=false ./gradlew bootRun) &

# 프론트
(cd frontend && npm run dev) &

wait
