# algoforge-frontend

React 18 + TypeScript + Vite + Tailwind + React Router + TanStack Query + Zustand + Monaco Editor.

## 실행

```bash
npm install
npm run dev
# http://localhost:5173
```

`vite.config.ts`에 `/api` → `http://localhost:8080` 프록시가 설정되어 있어 백엔드와 함께 개발할 수 있습니다.

## 폴더

```
src/
├── App.tsx                # 라우트 정의
├── main.tsx
├── routes/guards/         # AuthGuard, AdminGuard
├── layouts/               # UserLayout, AdminLayout
├── pages/                 # auth, home, problems, submissions, mypage, admin
├── components/            # common, editor, problem, submission, admin
├── api/                   # axios client + 도메인별 api
├── hooks/                 # useAuth, useSubmissionStatus...
├── stores/                # zustand 전역 상태 (authStore)
├── types/                 # 도메인 타입
├── utils/
└── styles/index.css       # tailwind entry
```

## 상태 관리

- 서버 상태: `@tanstack/react-query`
- 전역 클라이언트 상태(인증 등): `zustand`
- 폼: `react-hook-form` + `zod`

## 빌드

```bash
npm run build
# dist/ 결과를 Nginx 정적 서빙
```

## 문제 콘텐츠 렌더링

문제 본문/예제 설명은 모두 `components/markdown/ProblemMarkdown` 을 통해 렌더링합니다.

| 기능 | 라이브러리 |
|------|-----------|
| 마크다운 (GFM) | `react-markdown`, `remark-gfm` |
| 줄바꿈 보존 (single newline → `<br>`) | `remark-breaks` |
| KaTeX 수식 (`$...$`, `$$...$$`) | `remark-math` + `rehype-katex` |
| 이미지 | 표준 `![alt](https://...)` (https/http/`/` 만 허용) |

`<li>` 안 등 인라인 자리에서는 `inline` prop 으로 `<p>` 마진을 제거합니다.
KaTeX 의 `strict: 'ignore'` 로 알 수 없는 매크로가 들어와도 빨간 에러 박스 대신 원문이 그대로 보입니다.
스타일은 `main.tsx` 에서 `katex/dist/katex.min.css` 를 전역으로 import 합니다.

## 문제 목록 UI

`/problems` 목록은 `category`, `difficulty`, `ai`, `keyword` 쿼리스트링을 그대로 백엔드에 전달합니다.
로그인 사용자는 응답 `userStatus` 를 받아 `ProblemUserStatusBadge` 로 "해결" / "틀림" 배지를 표시합니다.
