# AlgoForge

백준과 유사한 알고리즘 문제 풀이 플랫폼 + AI 기반 문제 생성 / 단계별 힌트 / 반례 생성을 결합한 학습 서비스의 모노레포입니다.

## 모노레포 구조

```
algoforge/
├── backend/         # Spring Boot 3.4 / JDK 23 / Gradle (REST API + JWT)
├── frontend/        # React 18 + Vite + TypeScript + Tailwind + Monaco Editor
├── ai-server/       # FastAPI + LangChain + Gemini (문제 생성 / 피드백 / 반례)
├── judge-worker/    # Spring Boot 채점 워커 (RabbitMQ 컨슈머 + Docker 샌드박스)
├── judge-images/    # 언어별 채점용 Docker 이미지 (java/python/cpp/node)
├── infra/           # docker-compose, Nginx, systemd, 배포 스크립트
└── docs/            # 아키텍처/운영 문서
```

## 기술 스택

| 영역 | 스택 |
|------|------|
| 백엔드 API | Java 23, Spring Boot 3.4, Gradle, Spring Security(JWT), Spring Data JPA, Querydsl, Flyway |
| 프론트엔드 | React 18, TypeScript, Vite, TailwindCSS, React Router, TanStack Query, Zustand, Monaco Editor |
| AI 서버 | Python 3.12, FastAPI, LangChain, langchain-google-genai (Gemini) |
| 채점 워커 | Java 23, Spring Boot, RabbitMQ Client, Docker CLI 호출 |
| DB / 큐 / 캐시 | PostgreSQL 16, Redis 7, RabbitMQ 3, MinIO (S3 호환) |
| 운영 | Ubuntu 22.04, Nginx, systemd, Docker / Docker Compose |

## 빠른 시작 (개발 환경)

### 1. 사전 요구
- Docker Desktop (Windows/Mac) 또는 Docker Engine
- JDK 23 (Temurin 권장)
- Node.js 20 LTS
- Python 3.12

### 2. 인프라 기동 (Postgres / Redis / RabbitMQ / MinIO)
```bash
cd infra
docker compose up -d
```
- Postgres: `localhost:5432` (db: `algoforge`, user: `algoforge`, pw: `algoforge`)
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672` (관리 UI: http://localhost:15672 — guest/guest)
- MinIO: http://localhost:9001 (minioadmin/minioadmin)

### 3. 백엔드
```bash
cd backend
./gradlew bootRun
# http://localhost:8080/actuator/health
```

### 4. 프론트엔드
```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

### 5. AI 서버
```bash
cd ai-server
python -m venv .venv && source .venv/bin/activate    # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env   # GEMINI_API_KEY 채우기
uvicorn app.main:app --reload --port 8000
# http://localhost:8000/health
```

### 6. Judge Worker
```bash
cd judge-worker
./gradlew bootRun
```

## 주요 포트

| 서비스 | 포트 |
|--------|------|
| Frontend (dev) | 5173 |
| Backend API | 8080 |
| AI Server | 8000 |
| Judge Worker | (헬스용 8090) |
| Postgres | 5432 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 (UI) |
| MinIO | 9000 / 9001 (UI) |

## 보안 주의사항 (반드시 읽어주세요)

1. **정답 코드와 숨김 테스트케이스는 일반 사용자 API에 절대 포함되지 않습니다.** DTO를 분리해 사용합니다 (`ProblemDetailResponse` vs `AdminProblemDetailResponse`).
2. **AI Server는 내부망 통신만 허용**합니다. `INTERNAL_AI_TOKEN` 헤더 검증.
3. **사용자 코드는 격리된 Docker 컨테이너에서만 실행**됩니다 (`--network=none`, `--read-only`, cgroup 제한).
4. **외부 사이트 문제를 그대로 복제 저장하지 않습니다.** 메타데이터만 보관하거나 AI가 새 문제를 재생성합니다.
5. `.env` 파일에는 절대 git 커밋 금지. `.env.example`만 커밋합니다.

## 문서

- [아키텍처](docs/ARCHITECTURE.md)
- [DB 스키마](backend/src/main/resources/db/migration) (Flyway `V*.sql`)
- [API 명세](docs/API.md)
- [보안 정책](docs/SECURITY.md)
- [운영 런북](docs/RUNBOOK.md) · [인프라·배포 스크립트](infra/README.md)
- [Ubuntu VM 단일 서버 배포 가이드](docs/DEPLOYMENT_UBUNTU.md)

## 라이선스

내부 학습/개발용. 외부 공개 시 라이선스 별도 결정 필요.
