#!/usr/bin/env bash
# Postgres 백업 (cron 일일 권장)
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/algoforge}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
PGUSER="${PGUSER:-algoforge}"
PGDATABASE="${PGDATABASE:-algoforge}"
PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"

mkdir -p "$BACKUP_DIR"
TS=$(date +%Y%m%d_%H%M%S)
OUT="$BACKUP_DIR/${PGDATABASE}_${TS}.sql.gz"

PGPASSWORD="${PGPASSWORD:-}" pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$PGDATABASE" \
    | gzip -9 > "$OUT"

echo "[OK] backup -> $OUT"

# 보관 기간 초과 파일 정리
find "$BACKUP_DIR" -name '*.sql.gz' -mtime +"$RETENTION_DAYS" -delete
echo "[OK] cleaned files older than $RETENTION_DAYS days"
