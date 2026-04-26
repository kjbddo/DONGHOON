# API 명세 (요약)

상세 명세는 추후 OpenAPI(`/v3/api-docs`)와 Swagger UI(`/swagger-ui/index.html`)로 자동 생성됩니다.

## Auth
- POST /api/auth/signup
- POST /api/auth/login → access + refresh
- POST /api/auth/refresh → rotation
- POST /api/auth/logout (Auth)

## Problem (사용자)
- GET /api/problems
  - Query: `difficulty`, `ai`(=aiOnly, boolean), `category`, `keyword`, `page`, `size`, `sort`
  - `category` 는 `problem_categories.name` 과 대소문자 무시 일치(LOWER)로 비교.
  - 응답 요소(`ProblemSummaryResponse`) 에 `userStatus` 추가:
    - 비로그인 또는 시도 이력 없음 → `null`
    - `user_solved_problems` 에 존재 → `SOLVED`
    - `submissions` 에 시도 이력은 있지만 ACCEPTED 가 없음 → `WRONG`
- GET /api/problems/{id}
- POST /api/problems/{id}/bookmark (Auth)
- POST /api/problems/{id}/report (Auth)

### 문제 본문 렌더링 규약
- `description` / `inputDescription` / `outputDescription` / `constraints[*]` /
  `examples[*].explanation` 은 마크다운(GFM) + KaTeX 수식이 적용된 형태로 렌더링됩니다.
  - 인라인 수식: `$x \\le 10^9$`, 블록 수식: `$$\\sum_{i=1}^{n} a_i$$`
  - 외부 이미지: `![alt](https://...)` (https/http/`/` 절대경로만 허용, 그 외는 안전장치로 대체 텍스트 출력)
- 백엔드는 AI 가 생성한 텍스트의 literal `\\n` / `\\r` / `\\t` 를 실제 제어문자로 정규화한 뒤 저장합니다
  (`com.algoforge.backend.ai.util.AiTextNormalizer`). 수식 영역(`$...$`, `$$...$$`)은 보존.

## Submission
- POST /api/submissions (Auth)
- GET /api/submissions (Auth)
- GET /api/submissions/{id} (Auth, owner)
- GET /api/submissions/{id}/results (Auth)

## AI Feedback / CounterExample
- POST /api/submissions/{submissionId}/ai-feedback (Auth, owner)
- POST /api/submissions/{submissionId}/counter-examples (Auth, owner)

## Admin
- GET    /api/admin/problems
- POST   /api/admin/problems
- PUT    /api/admin/problems/{id}
- PATCH  /api/admin/problems/{id}/status
- GET    /api/admin/problems/{id}/revisions
- GET    /api/admin/problems/{id}/solutions
- POST   /api/admin/problems/{id}/solutions
- PUT    /api/admin/solutions/{solutionId}
- DELETE /api/admin/solutions/{solutionId}
- POST   /api/admin/problem-imports
- GET    /api/admin/problem-imports/{id}
- GET    /api/admin/reports
