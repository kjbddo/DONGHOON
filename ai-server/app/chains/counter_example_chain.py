from pathlib import Path

from langchain_core.output_parsers import JsonOutputParser

from app.core.gemini_client import get_chat_model
from app.core.retry import generate_with_validation
from app.schema.counter_example import (
    CounterExampleRequest,
    CounterExampleResponseSchema,
)

_PROMPT = (Path(__file__).parent.parent / "prompts" / "counter_example.v1.txt").read_text(encoding="utf-8")


def _build_prompt(req: CounterExampleRequest, previous_error: str | None) -> str:
    previous_section = (
        ""
        if not previous_error
        else f"\n# 직전 시도에서 다음 검증 오류가 있었다.\n```\n{previous_error}\n```"
    )
    return _PROMPT.format(
        problem_title=req.problem_title,
        description=req.description,
        constraints=", ".join(req.constraints),
        language=req.language,
        user_code=req.user_code,
        failed_test_excerpt=req.failed_test_excerpt or "(없음)",
        previous_error_section=previous_section,
    )


async def generate_counter_examples(req: CounterExampleRequest) -> CounterExampleResponseSchema:
    model = get_chat_model(temperature=0.5)
    parser = JsonOutputParser()

    async def invoke(previous_error: str | None) -> dict:
        prompt = _build_prompt(req, previous_error)
        message = await model.ainvoke(prompt)
        return parser.parse(message.content if hasattr(message, "content") else str(message))

    return await generate_with_validation(CounterExampleResponseSchema, invoke)
