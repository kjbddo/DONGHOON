import logging
from typing import Awaitable, Callable, Type, TypeVar

from pydantic import BaseModel, ValidationError

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)


class AIGenerationFailed(Exception):
    pass


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
                # JsonOutputParser 미사용 시
                import json
                raw = json.loads(raw)
            return schema_cls.model_validate(raw)
        except (ValidationError, ValueError) as e:
            last_error = str(e)
            logger.warning("AI 응답 검증 실패 (시도 %d/%d): %s", attempt, max_attempts, last_error)
    raise AIGenerationFailed(f"AI generation failed after {max_attempts} attempts: {last_error}")
