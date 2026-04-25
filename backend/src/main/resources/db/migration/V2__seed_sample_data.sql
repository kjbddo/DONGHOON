-- ============================================================================
-- V2: 카테고리/태그 + 샘플 문제 시드
-- (관리자 계정은 BCrypt 해시 안전성 때문에 ApplicationRunner에서 코드로 생성)
-- ============================================================================

-- ===== Categories =====
INSERT INTO problem_categories (name) VALUES
    ('구현'),
    ('자료구조'),
    ('정렬'),
    ('그래프'),
    ('동적계획법')
ON CONFLICT (name) DO NOTHING;

-- ===== Tags =====
INSERT INTO problem_tags (name) VALUES
    ('implementation'),
    ('math'),
    ('sorting'),
    ('bfs'),
    ('dfs'),
    ('dp'),
    ('greedy')
ON CONFLICT (name) DO NOTHING;

-- ===== 샘플 문제 1: 두 수의 합 =====
-- 가장 단순한 BRONZE 난이도 / 표준입출력 검증용.
INSERT INTO problems (
    title, slug, description, input_description, output_description,
    constraints, examples,
    time_limit_ms, memory_limit_mb,
    difficulty, status, source_type, is_ai_generated,
    created_at, updated_at
) VALUES (
    '두 수의 합',
    'two-sum-basic',
    '두 정수 A와 B가 주어질 때, A + B를 출력하는 프로그램을 작성하시오.',
    '첫째 줄에 두 정수 A와 B가 공백으로 구분되어 주어진다.',
    '첫째 줄에 A + B의 값을 출력한다.',
    '["-1,000,000,000 ≤ A, B ≤ 1,000,000,000"]'::jsonb,
    '[
       {"input": "1 2", "output": "3", "explanation": "1 + 2 = 3"},
       {"input": "100 200", "output": "300", "explanation": null}
     ]'::jsonb,
    1000, 256,
    'BRONZE', 'PUBLIC', 'ADMIN_CREATED', FALSE,
    now(), now()
)
ON CONFLICT (slug) DO NOTHING;

-- 카테고리/태그 매핑 (slug 기반 안전 매핑)
INSERT INTO problem_category_map (problem_id, category_id)
SELECT p.id, c.id
FROM problems p
CROSS JOIN problem_categories c
WHERE p.slug = 'two-sum-basic' AND c.name = '구현'
ON CONFLICT DO NOTHING;

INSERT INTO problem_tag_map (problem_id, tag_id)
SELECT p.id, t.id
FROM problems p
CROSS JOIN problem_tags t
WHERE p.slug = 'two-sum-basic' AND t.name IN ('implementation', 'math')
ON CONFLICT DO NOTHING;

-- 테스트 케이스 (공개 2건 + hidden 1건)
INSERT INTO test_cases (problem_id, input, expected_output, is_hidden, seq)
SELECT p.id, '1 2', '3', FALSE, 1 FROM problems p WHERE p.slug = 'two-sum-basic';

INSERT INTO test_cases (problem_id, input, expected_output, is_hidden, seq)
SELECT p.id, '100 200', '300', FALSE, 2 FROM problems p WHERE p.slug = 'two-sum-basic';

INSERT INTO test_cases (problem_id, input, expected_output, is_hidden, seq)
SELECT p.id, '-1000000000 1000000000', '0', TRUE, 3 FROM problems p WHERE p.slug = 'two-sum-basic';
