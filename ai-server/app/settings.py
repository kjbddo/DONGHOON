from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_env: str = Field(default="local")
    port: int = Field(default=8000)

    gemini_api_key: str = Field(default="")
    gemini_model: str = Field(default="gemini-2.0-flash-exp")

    internal_ai_token: str = Field(default="dev-internal-token-change-me")
    log_level: str = Field(default="INFO")

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    return Settings()
