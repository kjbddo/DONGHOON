#!/usr/bin/env bash
# 프론트엔드 정적 파일 배포
# 사용법: ./deploy-frontend.sh ./frontend/dist
set -euo pipefail

DIST="${1:-./frontend/dist}"
DEPLOY_DIR="/srv/algoforge/frontend/dist"

if [[ ! -d "$DIST" ]]; then
  echo "[ERROR] dist 폴더가 없습니다: $DIST" >&2
  exit 1
fi

echo "[1/2] rsync"
sudo rsync -av --delete "$DIST/" "$DEPLOY_DIR/"

echo "[2/2] Nginx reload"
sudo nginx -t && sudo systemctl reload nginx

echo "[OK] 프론트엔드 배포 완료"
