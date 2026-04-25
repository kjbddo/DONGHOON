# API 명세 (요약)

상세 명세는 추후 OpenAPI(`/v3/api-docs`)와 Swagger UI(`/swagger-ui/index.html`)로 자동 생성됩니다.

## Auth
- POST /api/auth/signup
- POST /api/auth/login → access + refresh
- POST /api/auth/refresh → rotation
- POST /api/auth/logout (Auth)

## Problem (사용자)
- GET /api/problems
- GET /api/problems/{id}
- POST /api/problems/{id}/bookmark (Auth)
- POST /api/problems/{id}/report (Auth)

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
