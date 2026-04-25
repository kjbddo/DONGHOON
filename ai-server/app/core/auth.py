from fastapi import Header, HTTPException, status

from app.settings import get_settings


async def verify_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    settings = get_settings()
    if not x_internal_token or x_internal_token != settings.internal_ai_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid internal token",
        )
