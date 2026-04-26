import json
import re
from pathlib import Path
from typing import Any

from langchain_core.output_parsers import JsonOutputParser

from app.core.gemini_client import get_chat_model
from app.core.retry import generate_with_validation
from app.schema.problem import GeneratedProblemSchema, GenerateProblemRequest

_PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "problem_gen.v1.txt"
_PROMPT = _PROMPT_PATH.read_text(encoding="utf-8")


# ─────────────────────────────────────────────────────────────────────────────
# 응답 정규화
# Gemini 응답이 한 단계만 풀려 들어와 description/inputDescription 등에 literal
# `\n`, `\t`, `\r` 두 글자가 그대로 남는 경우가 자주 발생한다.
# 그렇다고 모든 백슬래시를 풀어버리면 `\max`, `\le` 같은 KaTeX 명령이 깨지므로,
# `$...$` / `$$...$$` 수식 영역은 건드리지 않고 그 바깥 영역의 `\n`/`\t`/`\r` 만
# 실제 제어문자로 치환한다.
# ─────────────────────────────────────────────────────────────────────────────
_MATH_SPLIT = re.compile(r"(\$\$[\s\S]*?\$\$|\$[^\n$]+\$)")


def _normalize_text(value: str) -> str:
    parts = _MATH_SPLIT.split(value)
    for i in range(0, len(parts), 2):
        parts[i] = (
            parts[i]
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .replace("\\t", "\t")
        )
    return "".join(parts)


def _normalize_payload(node: Any) -> Any:
    if isinstance(node, str):
        return _normalize_text(node)
    if isinstance(node, list):
        return [_normalize_payload(x) for x in node]
    if isinstance(node, dict):
        return {k: _normalize_payload(v) for k, v in node.items()}
    return node


def _build_prompt(req: GenerateProblemRequest, previous_error: str | None) -> str:
    previous_section = (
        ""
        if not previous_error
        else f"\n# 이전 시도에서 너는 다음 검증 오류를 냈다. 이번에는 반드시 스키마를 정확히 지켜라.\n```\n{previous_error}\n```"
    )
    return _PROMPT.format(
        category=req.category or "자유",
        difficulty=req.difficulty or "Silver",
        topic_hint=req.topic_hint or "(없음)",
        source_metadata=json.dumps(req.source_metadata, ensure_ascii=False) if req.source_metadata else "(없음)",
        previous_error_section=previous_section,
    )


async def generate_problem(req: GenerateProblemRequest) -> GeneratedProblemSchema:
    model = get_chat_model(temperature=0.7)
    parser = JsonOutputParser()

    async def invoke(previous_error: str | None) -> dict:
        prompt = _build_prompt(req, previous_error)
        message = await model.ainvoke(prompt)
        raw = parser.parse(message.content if hasattr(message, "content") else str(message))
        return _normalize_payload(raw)

    return await generate_with_validation(GeneratedProblemSchema, invoke)
