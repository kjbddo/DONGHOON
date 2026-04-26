# 아키텍처

```
┌─────────────────┐
│   브라우저       │
└────────┬────────┘
         │ HTTPS
   ┌─────▼─────┐
   │   Nginx    │ ── 정적(React) + Reverse Proxy
   └──┬──────┬──┘
      │      │
 ┌────▼──┐ ┌─▼─────────────┐
 │React  │ │ Spring Boot    │
 │SPA    │ │ API (JWT/REST) │
 └───────┘ └──┬──────┬──────┘
              │      │
       ┌──────┘      └───────────┐
       │                          │
 ┌─────▼─────────┐         ┌──────▼──────┐
 │ AI Server     │         │ RabbitMQ    │
 │ (FastAPI +    │         │ judge.queue │
 │  LangChain +  │         └──────┬──────┘
 │  Gemini)      │                │
 └───────────────┘         ┌──────▼──────┐
                           │ Judge Worker│
                           │ + Docker    │
                           │   sandbox   │
                           └──────┬──────┘
                                  │
                  ┌───────────────▼───────────────┐
                  │  PostgreSQL / Redis / MinIO   │
                  └───────────────────────────────┘
```

자세한 컴포넌트 책임은 루트 README와 각 서브프로젝트 README를 참고하세요.

## 문제 콘텐츠 파이프라인 (마크다운 + KaTeX)

문제 본문은 다음 단계를 거쳐 생성·저장·렌더링됩니다.

```
관리자 입력 또는 AI 생성
        │
        ▼
┌─────────────────────────────────────────────┐
│ ai-server (FastAPI + Gemini)                │
│  - prompts/problem_gen.v1.txt 의 "수식 표기   │
│    규칙" 로 KaTeX 작성을 강제                 │
│  - JsonOutputParser → 응답 정규화             │
│    (`\n`/`\t`/`\r` literal → 제어문자)        │
│  - `$...$` / `$$...$$` 수식 영역은 보존       │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ backend (Spring Boot)                        │
│  - AiTextNormalizer 로 동일 정규화 한 번 더    │
│  - description / input·outputDescription /   │
│    constraints / examples / testCases 모두    │
│    적용                                       │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ DB (PostgreSQL)                              │
│  - problems.description (text)               │
│  - problems.constraints (jsonb)              │
│  - problems.examples (jsonb)                 │
│  - test_cases.input / expected_output (text) │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ frontend (React)                             │
│  components/markdown/ProblemMarkdown          │
│  - react-markdown                             │
│  - remark-gfm  (테이블 등)                    │
│  - remark-breaks (single newline → <br>)      │
│  - remark-math + rehype-katex                 │
│    · KaTeX 에러는 무시(strict: ignore)         │
│  - inline 모드: <li> 안에서 <p> 마진 제거       │
└─────────────────────────────────────────────┘
```

## 문제 목록·풀이 상태 (SOLVED / WRONG)

`GET /api/problems` 는 인증된 사용자에 대해 다음을 추가로 반환합니다.

- `user_solved_problems` 에 존재하는 문제 → `userStatus = SOLVED`
- 위 목록에는 없지만 `submissions` 테이블에 시도 이력이 있는 문제 → `userStatus = WRONG`
- 그 외(비로그인 포함) → `userStatus = null`

`SubmissionRepository.findAttemptedProblemIds` 와
`UserSolvedProblemRepository.findSolvedProblemIds` 두 쿼리만 한 페이지 단위로 호출해 N+1 을 피합니다.
