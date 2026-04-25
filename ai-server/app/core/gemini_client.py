from functools import lru_cache

from langchain_google_genai import ChatGoogleGenerativeAI

from app.settings import get_settings


@lru_cache
def get_chat_model(temperature: float = 0.7) -> ChatGoogleGenerativeAI:
    settings = get_settings()
    return ChatGoogleGenerativeAI(
        model=settings.gemini_model,
        google_api_key=settings.gemini_api_key,
        temperature=temperature,
        # JSON 모드 강제 (Gemini 1.5+/2.x 지원)
        model_kwargs={"generation_config": {"response_mime_type": "application/json"}},
    )
