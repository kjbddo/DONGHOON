# infra

로컬 개발용 `docker-compose`와 운영에 옮겨 쓰는 Nginx / systemd / 배포 스크립트 모음.

## docker-compose (개발)

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

- 볼륨: DB·큐·Redis·MinIO 데이터 보존. **완전 초기화**는 `docker compose down -v` (데이터 삭제).
- `docker-compose.yml`의 포트/계정이 `backend` 기본 `application.yml`의 `DB_*` / `RABBITMQ_*` 와 맞는지 확인.

| 서비스 | 포트(기본) | 메모 |
|--------|------------|------|
| PostgreSQL | 5432 | `algoforge/algoforge` |
| Redis | 6379 | |
| RabbitMQ | 5672, 15672 UI | guest/guest |
| MinIO | 9000 API, 9001 Console | (현재 백엔드 필수는 아닐 수 있음) |

## 운영에 복사하는 파일

| 경로 | 용도 |
|------|------|
| `nginx/algoforge.conf` | `sites-available` + `ln -s` — `server_name`, TLS 경로, **SSE** location |
| `systemd/algoforge-*.service` | `/etc/systemd/system/` — `User`, `EnvironmentFile`, `ReadWritePaths`를 서버에 맞게 조정 |
| `scripts/deploy-backend.sh` | JAR 경로, `DEPLOY_DIR`, `curl` 헬스 URL |
| `scripts/deploy-frontend.sh` | `rsync` 대상, Nginx `root` |
| `scripts/backup-db.sh` | `BACKUP_DIR`, `PG*` 환경 변수, cron |

**첫 설치 시 (요약)**:

1. 시스템 유저 `algoforge`, 디렉터리 `/srv/algoforge/{backend,frontend/dist,…}/logs` 생성.
2. `/etc/algoforge/backend.env` 등 — DB·JWT·AI 토큰·RabbitMQ (루트 README·`docs/SECURITY.md` 참고).
3. `systemctl enable --now` 각 서비스.
4. Nginx `nginx -t` 후 reload.

## 스크립트 빠른 참조

- **deploy-backend.sh** `[JAR]`: JAR → `/srv/algoforge/backend/app.jar`, `restart`, 30×2s 헬스.
- **deploy-frontend.sh** `[dist]`: `rsync` → `/srv/algoforge/frontend/dist`, `nginx -t && reload`.
- **backup-db.sh**: `pg_dump` gzip, `find … -mtime`로 오래된 백업 삭제.

로그 경로·서비스 이름은 **스크립트 상단과 systemd 유닛**을 SSOT로 두고, 운영 서버에 맞게만 수정할 것.

## 관련 문서

- [운영 런북 (전체)](../docs/RUNBOOK.md)
- [보안](../docs/SECURITY.md)
