-- ============================================================================
-- V4: ai_call_logs 보강
--   - user_id 컬럼 추가 (FK, nullable: 시스템 호출은 NULL)
--   - 일일 quota 조회를 위한 (user_id, created_at) 복합 인덱스
--   - purpose 별 집계용 인덱스
-- ============================================================================

ALTER TABLE ai_call_logs
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ai_call_logs_user_time
    ON ai_call_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_call_logs_purpose_time
    ON ai_call_logs(purpose, created_at DESC);
