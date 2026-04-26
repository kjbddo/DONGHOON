# AlgoForge 운영 런북

프로덕션(Ubuntu 22.04, systemd, Nginx) 기준. 경로·포트는 `infra/systemd/*.service` 및 `application.yml`과 맞출 것.

## 1. 구성요소·기동 순서

| 구성 | 역할 | 비고 |
|------|------|------|
| PostgreSQL | 메인 DB | Flyway 마이그레이션 |
| Redis | JWT 블랙리스트 등 | |
| RabbitMQ | `judge.submission` / `judge.result` | 백엔드·worker 모두 AMQP |
| Nginx | 정적 `/`, `/api` 프록시, SSE | `buffering off` for submission stream |
| backend | REST API, SSE | `:8080` |
| judge-worker | 채점 컨슈머 | Docker CLI 필요, **호스트에 docker.sock** |
| ai-server | Gemini 연동 | 내부 토큰 `X-Internal-Token` |

권장 기동 순서: **DB/Redis/RabbitMQ** → **backend** → **judge-worker** → **ai-server** → **Nginx** (또는 배포 직전까지는 로컬 헬스만).

## 2. 서비스 제어 (systemd)

```bash
sudo systemctl start|stop|restart|status algoforge-backend
sudo systemctl start|stop|restart|status algoforge-judge
sudo systemctl start|stop|restart|status algoforge-ai
sudo nginx -t && sudo systemctl reload nginx
```

- 유닛·WorkingDirectory: `infra/systemd/algoforge-*.service` 참고.
- **환경 변수**: `EnvironmentFile=/etc/algoforge/backend.env` (필드는 아래 "주요 환경 변수"와 `backend/src/main/resources/application.yml`의 `${…}` 키와 대응).

## 3. 배포

### 3.1 백엔드 JAR

`infra/scripts/deploy-backend.sh` — 빌드된 JAR를 `/srv/algoforge/backend/app.jar`에 복사 후 `systemctl restart`, `actuator/health` 루프.

```bash
cd backend && ./gradlew clean bootJar
sudo /path/to/infra/scripts/deploy-backend.sh build/libs/algoforge-backend-*.jar
```

- 실패 시: 스크립트가 `app.jar.bak` 경로를 안내. `sudo cp app.jar.bak app.jar` 후 `restart`로 수동 롤백.
- `management.endpoints`에 `health`가 포함돼 있어야 함 (기본 `actuator/health`).

### 3.2 프론트 (정적)

```bash
cd frontend && npm run build
sudo /path/to/infra/scripts/deploy-frontend.sh dist
```

- Nginx `root`는 `deploy-frontend.sh`의 `DEPLOY_DIR`(` /srv/algoforge/frontend/dist`)과 일치해야 함.

### 3.3 Judge Worker / AI

동일하게 JAR(또는 venv) 배포 후 해당 systemd 재시작. **Worker는 Docker가 동작 중**이어야 제출이 끝까지 처리됨.

## 4. 헬스·스모크

| URL | 기대 |
|-----|------|
| `http://127.0.0.1:8080/actuator/health` | 200, `UP` |
| `http://127.0.0.1:8000/health` (ai-server) | 200 (구현에 따름) |
| `http://127.0.0.1:15672` (RabbitMQ UI) | 로그인 후 큐 |
| Nginx `GET /api/…` | TLS·프록시·SSE(제출 스트림) 점검 |

API 문서(개발/스테이징): `https://<host>/swagger-ui.html` (Bearer 토큰).

## 5. 주요 환경 변수 (참고)

`application.yml`의 `${…}` 항목을 `backend.env` 등에 맞춤.

- **필수/강력 권장**: `DB_*`, `JWT_SECRET` (긴 랜덤), `REDIS_*`, `RABBITMQ_*`, `AI_SERVER_URL`, `INTERNAL_AI_TOKEN` (ai-server와 동일), 관리자 시드: `ADMIN_INIT_*` (초기 1회).
- **AI 한도**: `AI_QUOTA_DAILY_PER_USER`, `AI_QUOTA_DAILY_GLOBAL`
- **문제 가져오기(라이선스)**: `algoforge.problem.import.licensed-allowed-hosts` (yml)로 도메인 제한
- **Judge(백엔드)**: `judge.submission-queue` / `judge.result-queue` — worker `application.yml`과 **큐 이름 일치** 필수.

## 6. RabbitMQ

- 큐: `judge.submission` (Backend → Worker), `judge.result` (Worker → Backend).
- 적체 시: Management UI에서 Ready/Unacked, Consumer 수 확인. **Worker 프로세스·Docker·DB 연결** 점검.
- Dead Letter: DLX `judge.dlx` (설정은 코드·compose 주석 확인).

## 7. DB 백업·복원

- 스크립트: `infra/scripts/backup-db.sh` — `pg_dump` → `gzip`, 보관일 `RETENTION_DAYS`.
- cron 예: `0 3 * * * PGPASSWORD=… /srv/algoforge/infra/scripts/backup-db.sh` (권한·전용 `~/.pgpass` 권장).
- 복원(개발/스테이징에서만 절차 검증): `gunzip -c ...sql.gz | psql -h … -U algoforge algoforge`

## 8. Judge Worker 운영 팁

- **Docker**: `judge` 유저에 `docker` 그룹 또는 rootless 정책에 맞게 권한 부여. 이미지는 `judge-images/`로 빌드.
- `algoforge.judge` in **worker** `application.yml`: `short-circuit: true`이면 케이스 **순차**만. **병렬**은 `short-circuit: false` + `parallel-test-cases`>1.
- **메모리 수치**: Linux에서 cgroup `memory.peak` 베스트에포트; OOM(137)은 한도로 보정. Windows 개발용 Worker는 0에 가깝게 나올 수 있음.

## 9. 로그

- systemd: `StandardOutput/StandardError=append:…` 경로(예: `/srv/algoforge/backend/logs/stdout.log`).
- 애플리케이션 로그 레벨: `logging.level.com.algoforge=DEBUG`는 트러블슈팅 시에만 일시.

## 10. 장애·트러블슈팅

| 증상 | 점검 |
|------|------|
| 제출이 PENDING/ JUDGING에서 멈춤 | Worker 기동, RabbitMQ `judge.submission` 소비, Docker, DB 연결, Worker 로그 |
| AI 502/한도 | `resilience4j`/`AiClient` 로그, `ai_call_logs`, `AI_QUOTA_*` |
| 401 대량 | JWT 시크릿/만료, Redis 블랙리스트, Nginx `Authorization` 전달 |
| SSE 끊김 | Nginx `proxy_buffering off` for `/api/submissions/*/stream` |
| import 403 (정책) | `IMPORT_BLOCKED` — `licensed-allowed-hosts`·`licenseAck`·`sourceUrl` |
| DB 마이그레이션 실패 | Flyway `flyway_schema_history`, 백업 후 수동 SQL은 최소화 |

## 11. 문서·코드 싱크

- 아키텍처: [ARCHITECTURE.md](./ARCHITECTURE.md)
- API: [API.md](./API.md)
- 보안: [SECURITY.md](./SECURITY.md)
- DB 스키마: `backend/src/main/resources/db/migration/V*.sql` (ERD는 마이그레이션/엔티티를 기준으로 유지)

## 12. 문제 본문/예제의 escape 깨짐 보정

AI 응답 누락된 escape 단계 때문에 `description` / `examples` 등에 두 글자 `\n`, `\t` 가
literal 로 저장되는 케이스가 발견될 수 있습니다. 신규 입력은 `AiTextNormalizer` (backend) 와
ai-server `chains/problem_gen_chain.py` 의 `_normalize_payload` 가 막지만, 과거 데이터는 1회성
스크립트로 보정합니다.

```bash
# 호스트에서 실행 (PostgreSQL 컨테이너 사용)
sudo python3 /srv/algoforge/infra/scripts/normalize_problem_text.py
# 또는 ad-hoc 스크립트 (수식 영역 $...$ 는 보존하고 그 외 영역의 \n/\r/\t 만 제어문자로)
```

- 대상 컬럼: `problems.description / input_description / output_description / constraints / examples`,
  `test_cases.input / expected_output`
- idempotent 하므로 여러 번 실행해도 안전. 잔존 `\` 매치 가운데 KaTeX 명령(`\le`, `\frac` 등)은
  의도적으로 보존되어야 하므로 변경되지 않습니다.
