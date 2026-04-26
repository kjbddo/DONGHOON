"""
LLM 추상화. Gemini ↔ OpenAI(gpt-5-nano 등) 교체 시 chains 코드는 변경하지 않도록
`get_chat_model()` 함수명/시그니처를 유지한다.
실패 시(OpenAI SDK 미설치 등)는 의미 있는 에러를 띄운다.
"""
from functools import lru_cache
from typing import Any

from app.settings import get_settings


@lru_cache
def get_chat_model(temperature: float = 0.7) -> Any:
    settings = get_settings()
    provider = (settings.llm_provider or "openai").lower()

    if provider == "openai":
        try:
            from langchain_openai import ChatOpenAI
        except ImportError as e:  # pragma: no cover
            raise RuntimeError(
                "langchain-openai 미설치 — `pip install -r requirements.txt` 후 재기동"
            ) from e

        kwargs: dict = {
            "model": settings.openai_model,
            "api_key": settings.openai_api_key,
            # JSON 모드 — gpt-4o/4.1/5 계열 지원. 이전 모델이면 model_kwargs로 빼기.
            "model_kwargs": {"response_format": {"type": "json_object"}},
        }
        # GPT-5 / o-시리즈 reasoning 모델은 temperature 기본값(1.0)만 허용.
        # langchain_openai ChatOpenAI의 default temperature가 0.7이라
        # 명시적으로 1.0을 박아 줘야 OpenAI API가 거부하지 않는다.
        m = (settings.openai_model or "").lower()
        is_reasoning = (
            m.startswith("gpt-5") or m.startswith("o1") or m.startswith("o3") or m.startswith("o4")
        )
        kwargs["temperature"] = 1.0 if is_reasoning else temperature

        return ChatOpenAI(**kwargs)

    if provider == "gemini":
        from langchain_google_genai import ChatGoogleGenerativeAI

        return ChatGoogleGenerativeAI(
            model=settings.gemini_model,
            google_api_key=settings.gemini_api_key,
            temperature=temperature,
            model_kwargs={"generation_config": {"response_mime_type": "application/json"}},
        )

    raise RuntimeError(f"unknown LLM provider: {provider}")
