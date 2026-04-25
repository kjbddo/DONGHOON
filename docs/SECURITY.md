# 보안 정책

## 코드 실행 샌드박스
- 모든 사용자 코드는 Docker 컨테이너에서만 실행
- 옵션: `--network=none --read-only --memory --cpus --pids-limit --cap-drop=ALL --security-opt=no-new-privileges --user 65534:65534`
- 출력 사이즈 64MB 상한
- 호스트 작업 디렉터리는 read-only 마운트만 허용

## 민감 정보
- `solutions` 테이블은 ROLE_ADMIN만 접근 가능 (`/api/admin/**`)
- 일반 `Problem*Response`에는 정답 코드/숨김 테스트케이스 절대 미포함
- 숨김 테스트케이스 결과는 pass/fail만 노출

## JWT
- Access 30분, Refresh 14일 + Rotation
- Refresh는 hash로만 DB 저장
- 로그아웃 시 refresh revoke + access jti 블랙리스트 (Redis TTL)

## AI Server
- `X-Internal-Token` 헤더로 보호
- Nginx에서 `/ai/*` 외부 요청 차단
- `GEMINI_API_KEY`는 AI Server에만 보관

## 감사
- `AdminActionLog`에 모든 관리자 변경/조회를 자동 기록
- 정답 코드 조회는 반드시 로그
