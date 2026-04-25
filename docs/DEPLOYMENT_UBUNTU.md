# Ubuntu 가상머신 단일 서버 배포 가이드

이 문서는 **Ubuntu 22.04 LTS** 가상머신에 AlgoForge 전 구성요소(인프라·백엔드·프론트·AI·채점 워커·Nginx)를 **한 대**에 올리는 순서를 정리한 것입니다.  
경로는 저장소의 `infra/systemd`, `infra/scripts`와 맞춥니다. **root 또는 sudo** 권한이 필요한 단계가 있습니다.

> **권장 사양:** 스테이징/가벼운 운영 기준 **4 vCPU / 8GB RAM / 60GB+ SSD** 이상. (최소 4GB RAM은 채점·JVM 동시에 쓰기에 빠듯할 수 있음)

---

## 0. 사전 준비

- VM에 **SSH**로 접속 가능한 계정(관리용).
- **도메인**이 있으면 DNS A 레코드를 VM 공인 IP에 연결(HTTPS 시).
- 프로젝트는 Git 클론하거나, 로컬에서 빌드한 산출물(JAR, `dist`)만 scp로 옮겨도 됨. 아래는 **서버에서 소스를 받아 빌드**하는 흐름을 기준으로 설명합니다.

---

## 1. 시스템 업데이트·기본 도구

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y ca-certificates curl git gnupg ufw \
    build-essential rsync
```

- **타임존(권장 UTC 또는 서비스 기준):**  
  `sudo timedatectl set-timezone Asia/Seoul` (또는 `UTC`)

---

## 2. 전용 Linux 사용자 `algoforge` 생성

서비스 프로세스는 root 대신 **전용 유저**로 돌리는 것을 권장합니다.

```bash
sudo adduser --disabled-password --gecos "" algoforge
```

이후 JAR·정적 파일·로그는 `/srv/algoforge/` 아래에 둡니다(아래 12절에서 디렉터리 생성).

---

## 3. Docker Engine 설치 (채점 워커 필수)

채점은 **Docker로 사용자 코드를 격리 실행**하므로, **호스트에 Docker**가 있어야 합니다.

```bash
# Docker 공식 문서에 맞는 설치(예: Ubuntu 22.04)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker algoforge
```

- 설치 후 **로그아웃/재접속**하거나, judge용으로는 `algoforge`가 `docker` 그룹에 있는지 확인합니다.
- `judge` 유닛(저장소 `infra/systemd/algoforge-judge.service`)은 `Group=docker`를 사용합니다. 환경에 맞게 조정해도 됩니다.
- `sudo systemctl enable --now docker`

**채점용 이미지 빌드(소스에 `judge-images/`가 있을 때, 나중 10절에서도 가능):**

```bash
# 예: Java 이미지
# cd /path/to/repo/judge-images/java && docker build -t algoforge-judge-java:latest .
```

DB의 `code_languages.docker_image`와 실제 태그가 일치해야 합니다.

---

## 4. JDK 23 설치 (백엔드·judge-worker 빌드/실행)

배포 서버는 **Temurin 23** 등 **JDK 23**이 필요합니다.

```bash
# 예: Adoptium 수동 설치(버전/URL은 공식 사이트 최신 기준)
# /usr/lib/jvm/jdk-23/ 에 설치됐다고 가정 → systemd 유닛의 ExecStart 경로와 맞출 것
```

- `infra/systemd/algoforge-backend.service` 는 예시로 `ExecStart=/usr/lib/jvm/jdk-23/bin/java` 를 가정합니다. **실제 `which java` / 설치 경로**에 맞게 수정하세요.

---

## 5. Node.js 20 (프론트 빌드)

```bash
# NodeSource 등 공식 권장 방법으로 Node 20 LTS 설치
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
```

---

## 6. Python 3.12+ (AI 서버)

```bash
sudo apt install -y python3.12 python3.12-venv
python3.12 --version
```

---

## 7. (선택) UFW 방화벽

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
# DB·Redis·RabbitMQ·AI·백엔드는 기본 127.0.0.1만 쓰면 외부에 안 열어도 됨
sudo ufw enable
sudo ufw status
```

- SSH 포트를 바꿨다면 `allow`에 반영.

---

## 8. Git으로 저장소 가져오기 (또는 scp)

```bash
sudo mkdir -p /srv/algoforge
sudo chown -R algoforge:algoforge /srv/algoforge
sudo -u algoforge -i
cd /srv/algoforge
git clone <YOUR_REPO_URL> app
cd app
```

이후 `/srv/algoforge/app` 을 **프로젝트 루트**로 둡니다(문서에서 `app` = 리포 루트).

---

## 9. Docker Compose로 DB·Redis·RabbitMQ 기동 (권장)

호스트에 Postgres를 직접 깔지 않고, **같은 VM 안에서** 컨테이너로 띄우는 방식이 기본 `application.yml`과 잘 맞습니다.

```bash
cd /srv/algoforge/app/infra
docker compose up -d
docker compose ps
```

- `docker-compose.yml` 기본: Postgres `localhost:5432`, DB/유저/비밀 `algoforge` / `algoforge`  
- Redis `localhost:6379`  
- RabbitMQ `localhost:5672`, 관리 UI `http://127.0.0.1:15672` (guest/guest) — **운영에서 비밀번호 변경** 권장.

> MinIO는 백엔드 필수 연동이 아닐 수 있으므로, 필요 없으면 `docker-compose.yml`에서 서비스를 주석하거나 compose 프로필로 분리해도 됩니다.

---

## 10. 백엔드·프론트·워커·AI 빌드 (algoforge 유저)

### 10.1 Gradle (백엔드 + judge-worker JAR)

프로젝트에 `gradlew`가 있으면:

```bash
cd /srv/algoforge/app/backend
./gradlew clean bootJar
# 산출물: build/libs/algoforge-backend-*-SNAPSHOT.jar (이름은 build.gradle에 따름)
```

`gradlew`가 없으면 [Gradle](https://gradle.org/install/) 설치 후 `gradle bootJar` 또는 래퍼 생성.

```bash
cd /srv/algoforge/app/judge-worker
./gradlew clean bootJar
# build/libs/...-SNAPSHOT.jar
```

### 10.2 프론트 (Vite `dist`)

```bash
cd /srv/algoforge/app/frontend
npm ci
# 프로덕션 API가 같은 도메인 /api 를 쓰는지: vite env 확인 (보통 baseURL /api)
npm run build
# 산출물: dist/
```

### 10.3 AI 서버 (가상환경 + 의존성)

```bash
cd /srv/algoforge/app/ai-server
python3.12 -m venv .venv
source .venv/bin/activate
pip install -U pip
pip install -r requirements.txt
deactivate
```

---

## 11. 런타임 디렉터리·파일 배치

`infra`의 systemd는 대략 다음을 가정합니다(경로는 **유닛 파일과 실제 JAR 이름**에 맞게 수정).

```bash
sudo mkdir -p /srv/algoforge/{backend,frontend/dist,judge-worker,ai-server,infra/scripts}/logs
sudo chown -R algoforge:algoforge /srv/algoforge
```

- 백엔드 JAR: 예) `cp backend/build/libs/*.jar /srv/algoforge/backend/app.jar`
- judge-worker JAR: 예) `cp judge-worker/build/libs/*.jar /srv/algoforge/judge-worker/worker.jar`
- 프론트: `rsync -av frontend/dist/ /srv/algoforge/frontend/dist/`
- AI: **앱 루트**를 `/srv/algoforge/ai-server` 로 두는 경우(심볼릭 링크 또는 rsync `ai-server` 전체)

```bash
# 예: 소스는 그대로 두고 링크만
sudo -u algoforge ln -sfn /srv/algoforge/app/ai-server /srv/algoforge/ai-server
```

또는 `rsync`로 `/srv/algoforge/ai-server`에 복사 후 `.venv`는 그 경로에서 다시 생성.

---

## 12. 환경 변수 파일 (`/etc/algoforge/*.env`)

**비밀번호·토큰은 절대 Git에 넣지 말고**, 서버에만 둡니다. `sudo chmod 600 /etc/algoforge/*.env`

### 12.1 `backend.env` (Spring) — `EnvironmentFile`로 읽힘

`application.yml`의 `${...}` 키와 맞춥니다. 예시:

```bash
# /etc/algoforge/backend.env (예시 — 값은 본인 것으로 전부 변경)
DB_URL=jdbc:postgresql://127.0.0.1:5432/algoforge
DB_USERNAME=algoforge
DB_PASSWORD=algoforge
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
JWT_SECRET=여기에_최소_64바이트_이상_랜덤_문자열
AI_SERVER_URL=http://127.0.0.1:8000
INTERNAL_AI_TOKEN=스프링과_AI서버_동일한_내부_비밀_문자열
SERVER_PORT=8080
```

- **`INTERNAL_AI_TOKEN`**: Google에서 받는 키가 **아님**. 직접 정한 **긴 랜덤 문자열**을 AI `.env`와 **완전 동일**하게.
- `JWT_SECRET`은 **반드시** 개발용 기본값이 아닌 **강한 값**으로 변경.

Spring Boot는 `KEY=value`를 자동으로 프로퍼티에 매핑하는 방식이 아니라, **대문자+언더스코어**를 `application.yml`의 `${...}`에 넘깁니다. `application.yml`이 이미 `DB_URL` 등을 참조하므로 위 키 이름이 맞습니다.  
(일부는 `export`로만 잡힌다면, systemd `Environment=`에 개별 지정해도 됩니다.)

**프로필이 필요하면** `Environment=SPRING_PROFILES_ACTIVE=prod` 등을 유닛에 추가.

### 12.2 `ai.env` (uvicorn / FastAPI)

```bash
# /etc/algoforge/ai.env
APP_ENV=prod
PORT=8000
GEMINI_API_KEY=Google_AI_Studio_또는_해당_제품의_API_키
GEMINI_MODEL=gemini-2.0-flash
INTERNAL_AI_TOKEN=스프링과_동일
LOG_LEVEL=INFO
```

- AI 서버는 `127.0.0.1`에만 바인딩(저장소 systemd 예시). 외부는 Nginx/방화벽에서 직접 8000을 열지 않는 것이 안전합니다.

### 12.3 `judge.env` (judge-worker)

```bash
# /etc/algoforge/judge.env
DB_URL=jdbc:postgresql://127.0.0.1:5432/algoforge
DB_USERNAME=algoforge
DB_PASSWORD=algoforge
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
JUDGE_WORKDIR=/var/lib/algoforge/judge
DOCKER_BIN=/usr/bin/docker
WORKER_PORT=8090
```

- `JUDGE_WORKDIR`는 디스크 여유 있게 잡고, `algoforge` 유저가 쓰기 가능하게:  
  `sudo mkdir -p /var/lib/algoforge/judge && sudo chown algoforge:algoforge /var/lib/algoforge/judge`

권한:

```bash
sudo chown root:root /etc/algoforge/*.env
sudo chmod 600 /etc/algoforge/*.env
# backend/judge 는 root가 읽는 EnvironmentFile — 보통 user 서비스는 root로 읽힘
```

---

## 13. systemd 유닛 설치

```bash
sudo cp /srv/algoforge/app/infra/systemd/algoforge-backend.service /etc/systemd/system/
sudo cp /srv/algoforge/app/infra/systemd/algoforge-judge.service /etc/systemd/system/
sudo cp /srv/algoforge/app/infra/systemd/algoforge-ai.service /etc/systemd/system/
```

- 각 파일의 **`ExecStart` JVM 경로**, **JAR 경로**, **User/Group**, **ReadWritePaths**를 실제 환경에 맞게 편집.
- `algoforge-backend.service`의 `ReadWritePaths`가 좁으면, 로그/작업 경로에 맞게 조정.
- `algoforge-judge.service`는 **Docker socket** 접근이 필요: `User=algoforge`, `Group=docker` 등.

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now algoforge-backend
sudo systemctl enable --now algoforge-judge
sudo systemctl enable --now algoforge-ai
```

**가동 확인:**

```bash
curl -sS http://127.0.0.1:8080/actuator/health
curl -sS http://127.0.0.1:8000/health
curl -sS http://127.0.0.1:8090/actuator/health
```

---

## 14. Nginx (HTTPS 권장)

```bash
sudo apt install -y nginx
sudo cp /srv/algoforge/app/infra/nginx/algoforge.conf /etc/nginx/sites-available/algoforge.conf
# server_name, ssl_certificate 경로를 본인 도메인·cert 경로로 수정
sudo ln -s /etc/nginx/sites-available/algoforge.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

- **SSE**: 제출 실시간 스트림에 대해 `proxy_buffering off`가 설정돼 있어야 합니다(샘플에 포함).
- **Let's Encrypt**는 `certbot --nginx` 등으로 인증서 발급 후 `server` 블록의 `ssl_certificate`와 맞출 것.

---

## 15. DB 백업(선택, cron)

```bash
sudo cp /srv/algoforge/app/infra/scripts/backup-db.sh /usr/local/bin/algoforge-backup-db.sh
sudo chmod +x /usr/local/bin/algoforge-backup-db.sh
# PGPASSWORD는 환경이나 .pgpass로 — RUNBOOK 참고
```

---

## 16. 운영 점검 체크리스트

- [ ] `docker compose ps` — postgres/redis/rabbitmq healthy  
- [ ] `systemctl status algoforge-backend algoforge-judge algoforge-ai`  
- [ ] `curl` health (위 13절)  
- [ ] 브라우저: `https://<도메인>/` — 프론트, `/api/...` — API, 로그인·문제 제출·채점 끝까지  
- [ ] `INTERNAL_AI_TOKEN` 일치(백엔드 vs AI) — AI 호출 403/401 시 의심  
- [ ] `JWT_SECRET` 변경됨(기본값 사용 금지)  
- [ ] `docs/RUNBOOK.md` — 장애·큐·로그 경로

---

## 17. 자주 겪는 문제

| 현상 | 확인 |
|------|------|
| 백엔드 기동 실패, Flyway 오류 | DB URL/비번, 5432 열림, compose 기동 |
| 제출이 안 끝남 | `algoforge-judge` 실행, `docker ps`, RabbitMQ `judge.submission` 소비, judge 로그 |
| AI 401/403 | `INTERNAL_AI_TOKEN` 양쪽 동일, `AI_SERVER_URL` (스킴·포트) |
| Permission denied (Docker) | `algoforge` → `docker` 그룹, 재로그인 |

---

## 18. 문서·스크립트 링크

- [RUNBOOK.md](./RUNBOOK.md) — 서비스 재시작, 로그, 큐, 백업
- [SECURITY.md](./SECURITY.md) — 토큰·비밀·노출 최소화
- [infra README](../infra/README.md) — compose, 스크립트 요약

이 가이드는 “한 VM에 올리는 기본 경로”이며, 보안·규모에 따라 DB 분리·워커 수평 확장·WAF 등을 추가하면 됩니다.
