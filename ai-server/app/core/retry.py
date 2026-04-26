import logging
from typing import Any, Awaitable, Callable, Type, TypeVar

from pydantic import BaseModel, ValidationError

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)


class AIGenerationFailed(Exception):
    pass


def _normalize_keys(obj: Any) -> Any:
    """LLM이 키 앞뒤에 공백/제어문자를 붙여 보내는 경우(GPT-5 nano 관찰)에 대비.
    dict 키만 재귀적으로 trim 한다. 값은 건드리지 않는다."""
    if isinstance(obj, dict):
        return {(k.strip() if isinstance(k, str) else k): _normalize_keys(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_normalize_keys(v) for v in obj]
    return obj


async def generate_with_validation(
    schema_cls: Type[T],
    invoke: Callable[[str | None], Awaitable[dict | str]],
    max_attempts: int = 3,
) -> T:
    """invoke(previous_error)를 max_attempts번 호출하며 schema_cls 검증을 통과한 결과를 반환."""
    last_error: str | None = None
    for attempt in range(1, max_attempts + 1):
        try:
            raw = await invoke(last_error)
            if isinstance(raw, str):
                import json
                raw = json.loads(raw)
            raw = _normalize_keys(raw)
            return schema_cls.model_validate(raw)
        except (ValidationError, ValueError) as e:
            last_error = str(e)
            logger.warning("AI 응답 검증 실패 (시도 %d/%d): %s", attempt, max_attempts, last_error)
    raise AIGenerationFailed(f"AI generation failed after {max_attempts} attempts: {last_error}")
