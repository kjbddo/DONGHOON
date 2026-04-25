import logging

from fastapi import FastAPI

from app.api import counter_example, feedback, problem_gen
from app.settings import get_settings

settings = get_settings()

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)

app = FastAPI(
    title="AlgoForge AI Server",
    version="0.1.0",
    description="LangChain + Gemini 기반 문제 생성 / 단계별 힌트 / 반례 생성 API",
)


@app.get("/health", tags=["meta"])
async def health() -> dict:
    return {"status": "UP", "service": "algoforge-ai-server", "env": settings.app_env}


app.include_router(problem_gen.router)
app.include_router(feedback.router)
app.include_router(counter_example.router)
