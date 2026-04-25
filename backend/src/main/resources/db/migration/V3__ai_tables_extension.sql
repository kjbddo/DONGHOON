-- ============================================================================
-- AlgoForge - AI 도메인 보강 (V3)
-- - counter_examples.user_id 컬럼 추가 (FK)
-- - 조회 성능을 위한 보조 인덱스 추가
-- ============================================================================

ALTER TABLE counter_examples
    ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_counter_examples_submission
    ON counter_examples(submission_id);

CREATE INDEX IF NOT EXISTS idx_counter_examples_problem_time
    ON counter_examples(problem_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_feedbacks_submission
    ON ai_feedbacks(submission_id);
