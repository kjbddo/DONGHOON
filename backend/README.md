# algoforge-backend

Spring Boot 3.4 / JDK 23 기반 메인 API 서버.

## 실행 (개발)

1. 인프라 기동
   ```bash
   cd ../infra && docker compose up -d
   ```
2. JDK 23이 설치되어 있어야 합니다 (Temurin 23 권장).
3. Gradle Wrapper 동기화 (최초 1회):
   ```bash
   gradle wrapper --gradle-version 8.10.2
   ```
   (호스트에 Gradle이 설치되지 않은 경우 IntelliJ에서 한 번 빌드하면 자동으로 wrapper가 받아집니다.)
4. 실행
   ```bash
   ./gradlew bootRun
   ```
5. 헬스체크
   - http://localhost:8080/api/ping
   - http://localhost:8080/actuator/health
   - http://localhost:8080/swagger-ui/index.html

## 패키지 구조

상세 설명은 루트 [README.md](../README.md) 와 [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)를 참고하세요.

```
com.algoforge.backend
├── AlgoForgeApplication
├── common/               (응답, 예외, 유틸, 헬스)
├── config/               (Web, Rabbit, Properties)
├── security/             (JWT, SecurityConfig, Handlers)
├── auth/                 (회원가입/로그인/토큰)
├── user/                 (사용자 도메인)
├── problem/              (문제)
├── category/, testcase/, language/, solution/
├── submission/, judge/   (제출/채점 큐)
├── ai/, feedback/, counterexample/  (AI 게이트웨이/저장)
├── importjob/, report/, bookmark/
├── admin/                (관리자 공통, 작업 로그)
└── stat/                 (랭킹/통계)
```

## 마이그레이션

`src/main/resources/db/migration/V*__*.sql` 형식으로 추가하면 부팅 시 자동 적용됩니다.
**운영 DB는 절대 `ddl-auto=update`로 변경하지 마세요.**
