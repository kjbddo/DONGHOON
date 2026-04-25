import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.chains.feedback_chain import generate_feedback
from app.core.auth import verify_internal_token
from app.core.retry import AIGenerationFailed
from app.schema.feedback import FeedbackRequest, FeedbackResponseSchema

router = APIRouter(prefix="/ai/feedback", tags=["AI Feedback"])
logger = logging.getLogger(__name__)


@router.post(
    "",
    response_model=FeedbackResponseSchema,
    dependencies=[Depends(verify_internal_token)],
)
async def feedback_endpoint(req: FeedbackRequest) -> FeedbackResponseSchema:
    try:
        return await generate_feedback(req)
    except AIGenerationFailed as e:
        logger.exception("피드백 생성 실패")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(e)) from e
