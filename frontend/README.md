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
