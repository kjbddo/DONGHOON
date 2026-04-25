import json
from pathlib import Path

from langchain_core.output_parsers import JsonOutputParser

from app.core.gemini_client import get_chat_model
from app.core.retry import generate_with_validation
from app.schema.problem import GeneratedProblemSchema, GenerateProblemRequest

_PROMPT_PATH = Path(__file__).parent.parent / "prompts" / "problem_gen.v1.txt"
_PROMPT = _PROMPT_PATH.read_text(encoding="utf-8")


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
        return parser.parse(message.content if hasattr(message, "content") else str(message))

    return await generate_with_validation(GeneratedProblemSchema, invoke)
