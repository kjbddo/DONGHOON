#!/usr/bin/env bash
# AlgoForge Backend 배포 스크립트 (단순 rolling)
# 사용법: ./deploy-backend.sh /path/to/built-app.jar
set -euo pipefail

JAR_SOURCE="${1:-./backend/build/libs/algoforge-backend-0.0.1-SNAPSHOT.jar}"
DEPLOY_DIR="/srv/algoforge/backend"
SERVICE="algoforge-backend"

if [[ ! -f "$JAR_SOURCE" ]]; then
  echo "[ERROR] JAR 파일을 찾을 수 없습니다: $JAR_SOURCE" >&2
  exit 1
fi

echo "[1/4] 백업"
sudo cp -f "$DEPLOY_DIR/app.jar" "$DEPLOY_DIR/app.jar.bak" 2>/dev/null || true

echo "[2/4] 새 JAR 복사"
sudo cp -f "$JAR_SOURCE" "$DEPLOY_DIR/app.jar"
sudo chown algoforge:algoforge "$DEPLOY_DIR/app.jar"

echo "[3/4] 서비스 재시작"
sudo systemctl restart "$SERVICE"

echo "[4/4] 헬스체크"
for i in {1..30}; do
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null; then
    echo "[OK] 배포 성공"
    exit 0
  fi
  sleep 2
done

echo "[ERROR] 헬스체크 실패. 롤백 후보: $DEPLOY_DIR/app.jar.bak"
exit 1
