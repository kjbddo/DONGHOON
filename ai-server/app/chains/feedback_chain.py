from pathlib import Path

from langchain_core.output_parsers import JsonOutputParser

from app.core.gemini_client import get_chat_model
from app.core.retry import generate_with_validation
from app.schema.feedback import FeedbackRequest, FeedbackResponseSchema

_PROMPT = (Path(__file__).parent.parent / "prompts" / "feedback.v1.txt").read_text(encoding="utf-8")


def _build_prompt(req: FeedbackRequest, previous_error: str | None) -> str:
    previous_section = (
        ""
        if not previous_error
        else f"\n# 직전 시도에서 다음 검증 오류가 있었다. 다시 정확한 JSON을 만들어라.\n```\n{previous_error}\n```"
    )
    return _PROMPT.format(
        problem_title=req.problem_title,
        description=req.description,
        input_description=req.input_description,
        output_description=req.output_description,
        constraints=", ".join(req.constraints),
        language=req.language,
        user_code=req.user_code,
        judge_status=req.judge_status,
        failed_test_excerpt=req.failed_test_excerpt or "(없음)",
        runtime_error_message=req.runtime_error_message or "(없음)",
        compile_error_message=req.compile_error_message or "(없음)",
        feedback_level=req.feedback_level,
        previous_error_section=previous_section,
    )


async def generate_feedback(req: FeedbackRequest) -> FeedbackResponseSchema:
    model = get_chat_model(temperature=0.4)
    parser = JsonOutputParser()

    async def invoke(previous_error: str | None) -> dict:
        prompt = _build_prompt(req, previous_error)
        message = await model.ainvoke(prompt)
        return parser.parse(message.content if hasattr(message, "content") else str(message))

    return await generate_with_validation(FeedbackResponseSchema, invoke)
