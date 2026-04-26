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

## 응답 텍스트 정규화

`chains/problem_gen_chain.py::_normalize_payload` 가 LLM 응답을 schema 로 변환하기 직전에
모든 string 노드를 재귀로 훑으며 두 글자 literal escape (`\\n`, `\\r`, `\\t`) 를 실제 제어문자로
치환합니다. KaTeX 명령(`\\max`, `\\le`, `\\frac` ...) 을 보존하기 위해 `$...$` / `$$...$$`
영역은 변환 대상에서 제외합니다. backend 에도 같은 보호가 있습니다
(`com.algoforge.backend.ai.util.AiTextNormalizer`).

## 카테고리 자유 입력

`GenerateProblemRequest.category` 는 문자열 자유 입력입니다. 표준 enum 외에도 관리자가 직접 적은
한국어 카테고리(`그리디`, `정렬` 등) 를 그대로 통과시키며, 프롬프트의 "카테고리 처리 규칙" 이 입력값을
그대로 사용하도록 LLM 에 지시합니다. `자유` 가 들어오면 LLM 이 `SUGGESTED_CATEGORIES` 중 적합한
것을 고릅니다.

## 본문 작성 규칙(프롬프트가 강제)

`prompts/problem_gen.v1.txt` 의 "수식 표기 규칙" 섹션이 다음을 LLM 에 강제합니다.

- description / inputDescription / outputDescription / constraints / `examples[*].explanation`
  내 모든 수학 표기는 `$...$` 또는 `$$...$$` 로 둘러쌀 것.
- 같은 식을 raw 텍스트와 KaTeX 두 가지로 중복해 적지 말 것.
- `_`, `^` 가 들어가는 식은 반드시 KaTeX 로 감쌀 것 (마크다운 italic 충돌 방지).
- 그림이 필요하면 `![alt](https://...)` 마크다운 이미지로 삽입(URL 미확보 시 텍스트 묘사).
