-- ============================================================================
-- AlgoForge - Initial Schema (V1)
-- 본 마이그레이션은 MVP 단계의 핵심 테이블만 포함합니다.
-- 추가 도메인은 V2, V3 ... 에서 점진적으로 확장하세요.
-- ============================================================================

-- pgcrypto/citext가 필요한 경우만 활성화
CREATE EXTENSION IF NOT EXISTS citext;

-- ===== Users / Roles =====
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           CITEXT      NOT NULL UNIQUE,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles(name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- ===== Refresh Tokens =====
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ===== Code Languages =====
CREATE TABLE code_languages (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(20) NOT NULL UNIQUE,        -- JAVA / PYTHON / CPP / JAVASCRIPT
    display_name      VARCHAR(50) NOT NULL,
    file_extension    VARCHAR(10) NOT NULL,
    compile_required  BOOLEAN NOT NULL,
    compile_command   TEXT,
    run_command       TEXT NOT NULL,
    docker_image      VARCHAR(200) NOT NULL,
    time_multiplier   NUMERIC(4,2) NOT NULL DEFAULT 1.0
);
INSERT INTO code_languages(name, display_name, file_extension, compile_required, compile_command, run_command, docker_image, time_multiplier) VALUES
    ('JAVA',       'Java 21',     '.java', true,  'javac -encoding UTF-8 Main.java',                'java -Xss64m -Xmx{MEM}m -Dfile.encoding=UTF-8 Main', 'algoforge/judge-java:21',   2.00),
    ('PYTHON',     'Python 3.12', '.py',   false, NULL,                                              'python3 main.py',                                    'algoforge/judge-python:3.12', 3.00),
    ('CPP',        'C++20',       '.cpp',  true,  'g++ -O2 -std=c++20 -o main main.cpp',             './main',                                             'algoforge/judge-cpp:13',     1.00),
    ('JAVASCRIPT', 'Node.js 20',  '.js',   false, NULL,                                              'node --max-old-space-size={MEM} main.js',            'algoforge/judge-node:20',    2.00);

-- ===== Problems =====
CREATE TABLE problems (
    id                       BIGSERIAL PRIMARY KEY,
    title                    VARCHAR(255) NOT NULL,
    slug                     VARCHAR(255) NOT NULL UNIQUE,
    description              TEXT NOT NULL,
    input_description        TEXT NOT NULL,
    output_description       TEXT NOT NULL,
    constraints              JSONB NOT NULL DEFAULT '[]'::jsonb,
    examples                 JSONB NOT NULL DEFAULT '[]'::jsonb,
    time_limit_ms            INT NOT NULL DEFAULT 2000,
    memory_limit_mb          INT NOT NULL DEFAULT 256,
    difficulty               VARCHAR(20) NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source_type              VARCHAR(40) NOT NULL DEFAULT 'ADMIN_CREATED',
    is_ai_generated          BOOLEAN NOT NULL DEFAULT FALSE,
    ai_model_name            VARCHAR(100),
    ai_prompt_version        VARCHAR(50),
    generated_by_user_id     BIGINT REFERENCES users(id),
    quality_score            NUMERIC(3,2),
    report_count             INT NOT NULL DEFAULT 0,
    generated_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_problems_status_difficulty ON problems(status, difficulty);
CREATE INDEX idx_problems_source_type ON problems(source_type);
CREATE INDEX idx_problems_is_ai_generated ON problems(is_ai_generated);

-- ===== Categories / Tags =====
CREATE TABLE problem_categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE problem_tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE problem_category_map (
    problem_id  BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES problem_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (problem_id, category_id)
);

CREATE TABLE problem_tag_map (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag_id     BIGINT NOT NULL REFERENCES problem_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (problem_id, tag_id)
);

-- ===== Test Cases =====
CREATE TABLE test_cases (
    id              BIGSERIAL PRIMARY KEY,
    problem_id      BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input           TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_hidden       BOOLEAN NOT NULL DEFAULT FALSE,
    seq             INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_test_cases_problem_hidden ON test_cases(problem_id, is_hidden);

-- ===== Submissions =====
CREATE TABLE submissions (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id               BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    language_id              BIGINT NOT NULL REFERENCES code_languages(id),
    code                     TEXT NOT NULL,
    status                   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    execution_time_ms        INT,
    memory_used_kb           INT,
    compile_error_message    TEXT,
    runtime_error_message    TEXT,
    submitted_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    judged_at                TIMESTAMPTZ
);
CREATE INDEX idx_submissions_user_time ON submissions(user_id, submitted_at DESC);
CREATE INDEX idx_submissions_problem_status ON submissions(problem_id, status);
CREATE INDEX idx_submissions_pending ON submissions(status) WHERE status IN ('PENDING','JUDGING');

-- ===== Submission Test Case Results =====
CREATE TABLE submission_test_case_results (
    id                BIGSERIAL PRIMARY KEY,
    submission_id     BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    test_case_id      BIGINT NOT NULL REFERENCES test_cases(id),
    status            VARCHAR(30) NOT NULL,
    execution_time_ms INT,
    memory_used_kb    INT,
    output_excerpt    TEXT
);
CREATE INDEX idx_stcr_submission ON submission_test_case_results(submission_id);

-- ===== User Solved Problems =====
CREATE TABLE user_solved_problems (
    user_id                       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id                    BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    first_solved_at               TIMESTAMPTZ NOT NULL,
    first_accepted_submission_id  BIGINT NOT NULL REFERENCES submissions(id),
    attempt_count                 INT NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, problem_id)
);

-- ===== Solution (관리자 전용) =====
CREATE TABLE solutions (
    id                  BIGSERIAL PRIMARY KEY,
    problem_id          BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    language_id         BIGINT NOT NULL REFERENCES code_languages(id),
    solution_code       TEXT NOT NULL,
    explanation         TEXT,
    time_complexity     VARCHAR(50),
    space_complexity    VARCHAR(50),
    created_by_admin_id BIGINT REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_solutions_problem ON solutions(problem_id);

-- ===== Problem Revisions =====
CREATE TABLE problem_revisions (
    id                          BIGSERIAL PRIMARY KEY,
    problem_id                  BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    edited_by_admin_id          BIGINT REFERENCES users(id),
    previous_title              VARCHAR(255),
    previous_description        TEXT,
    previous_input_description  TEXT,
    previous_output_description TEXT,
    previous_constraints        JSONB,
    previous_examples           JSONB,
    previous_test_cases         JSONB,
    change_reason               TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_problem_revisions_problem_time ON problem_revisions(problem_id, created_at DESC);

-- ===== AI Feedbacks =====
CREATE TABLE ai_feedbacks (
    id                    BIGSERIAL PRIMARY KEY,
    submission_id         BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id            BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    feedback_level        SMALLINT NOT NULL,
    summary               TEXT,
    direction_hint        TEXT,
    counter_example_hint  TEXT,
    complexity_hint       TEXT,
    runtime_error_hint    TEXT,
    compile_error_hint    TEXT,
    raw_ai_response       JSONB,
    model_name            VARCHAR(100),
    prompt_version        VARCHAR(50),
    token_usage           INT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (submission_id, feedback_level)
);
CREATE INDEX idx_ai_feedbacks_user_time ON ai_feedbacks(user_id, created_at DESC);

-- ===== Counter Examples =====
CREATE TABLE counter_examples (
    id                  BIGSERIAL PRIMARY KEY,
    problem_id          BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    submission_id       BIGINT REFERENCES submissions(id) ON DELETE SET NULL,
    input               TEXT NOT NULL,
    expected_output     TEXT,
    reason              TEXT,
    related_constraint  TEXT,
    source              VARCHAR(20) NOT NULL DEFAULT 'AI',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ===== Bookmarks =====
CREATE TABLE bookmarks (
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, problem_id)
);

-- ===== Reports =====
CREATE TABLE problem_reports (
    id                BIGSERIAL PRIMARY KEY,
    problem_id        BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    reporter_user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type       VARCHAR(40) NOT NULL,
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_by        BIGINT REFERENCES users(id),
    handled_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_problem_reports_status ON problem_reports(status);

-- ===== Hint Usages =====
CREATE TABLE hint_usages (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id      BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    submission_id   BIGINT REFERENCES submissions(id) ON DELETE SET NULL,
    feedback_level  SMALLINT,
    used_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ===== Import =====
CREATE TABLE import_sources (
    id                      BIGSERIAL PRIMARY KEY,
    source_name             VARCHAR(50) NOT NULL UNIQUE,
    base_url                VARCHAR(500),
    import_policy           VARCHAR(40) NOT NULL DEFAULT 'METADATA_ONLY',
    license_type            VARCHAR(50),
    is_crawling_allowed     BOOLEAN NOT NULL DEFAULT FALSE,
    robots_txt_checked_at   TIMESTAMPTZ,
    terms_checked_at        TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE external_problem_references (
    id                    BIGSERIAL PRIMARY KEY,
    source_name           VARCHAR(50) NOT NULL,
    external_problem_id   VARCHAR(100) NOT NULL,
    title                 VARCHAR(255),
    difficulty            VARCHAR(20),
    tags                  JSONB,
    original_url          VARCHAR(500),
    import_mode           VARCHAR(40) NOT NULL,
    imported_problem_id   BIGINT REFERENCES problems(id),
    imported_by_admin_id  BIGINT REFERENCES users(id),
    imported_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_name, external_problem_id)
);

CREATE TABLE problem_import_jobs (
    id                            BIGSERIAL PRIMARY KEY,
    source_name                   VARCHAR(50) NOT NULL,
    requested_by_admin_id         BIGINT REFERENCES users(id),
    import_mode                   VARCHAR(40) NOT NULL,
    status                        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_external_problem_id VARCHAR(100),
    requested_url                 VARCHAR(500),
    result_problem_id             BIGINT REFERENCES problems(id),
    error_message                 TEXT,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at                  TIMESTAMPTZ
);
CREATE INDEX idx_import_jobs_status ON problem_import_jobs(status);

-- ===== Admin Action Log =====
CREATE TABLE admin_action_logs (
    id              BIGSERIAL PRIMARY KEY,
    admin_user_id   BIGINT NOT NULL REFERENCES users(id),
    action_type     VARCHAR(80) NOT NULL,
    target_type     VARCHAR(40),
    target_id       BIGINT,
    payload         JSONB,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_logs_admin_time ON admin_action_logs(admin_user_id, created_at DESC);
CREATE INDEX idx_admin_logs_target ON admin_action_logs(target_type, target_id);

-- ===== AI Call Logs (비용/응답 추적) =====
CREATE TABLE ai_call_logs (
    id                  BIGSERIAL PRIMARY KEY,
    purpose             VARCHAR(50) NOT NULL,    -- PROBLEM_GEN / FEEDBACK / COUNTER_EXAMPLE
    model_name          VARCHAR(100) NOT NULL,
    prompt_version      VARCHAR(50),
    request_payload     JSONB,
    response_payload    JSONB,
    input_tokens        INT,
    output_tokens       INT,
    cost_usd            NUMERIC(10,6),
    latency_ms          INT,
    success             BOOLEAN NOT NULL,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_call_logs_time ON ai_call_logs(created_at DESC);
