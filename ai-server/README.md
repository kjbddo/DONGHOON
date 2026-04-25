# algoforge-ai-server

FastAPI + LangChain + Google Gemini 기반 AI 게이트웨이.
Spring 백엔드만이 호출하도록 `X-Internal-Token` 헤더로 보호됩니다.

## 기능

| 엔드포인트 | 용도 |
|-----------|------|
| `POST /ai/problems/generate` | 카테고리/난이도 기반 새 문제 생성 (JSON 검증 + 재시도) |
| `POST /ai/feedback` | 단계별 힌트 (1=방향 / 2=놓치는 조건 / 3=반례 / 4=복잡도) |
| `POST /ai/counter-examples` | 코드 약점 반례 생성 |
| `GET /health` | 헬스 체크 |

## 실행

```bash
python -m venv .venv
source .venv/bin/activate            # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env                 # GEMINI_API_KEY 채우기
uvicorn app.main:app --reload --port 8000
```

문서: http://localhost:8000/docs

## 보안

- 모든 `/ai/*` 엔드포인트는 `X-Internal-Token` 헤더 검증.
- 외부 노출 금지. Nginx에서 `/ai/*`는 외부 접근 차단.
- `GEMINI_API_KEY`는 이 서버에만 둔다 (Spring/Frontend에는 절대 두지 않음).

## JSON 검증 + 재시도

모든 체인은 `app/core/retry.py::generate_with_validation`을 거칩니다.
Pydantic `ValidationError` 발생 시 에러 메시지를 다음 프롬프트에 주입해 재시도합니다 (최대 3회).
