import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.chains.problem_gen_chain import generate_problem
from app.core.auth import verify_internal_token
from app.core.retry import AIGenerationFailed
from app.schema.problem import GeneratedProblemSchema, GenerateProblemRequest

router = APIRouter(prefix="/ai/problems", tags=["AI Problem"])
logger = logging.getLogger(__name__)


@router.post(
    "/generate",
    response_model=GeneratedProblemSchema,
    dependencies=[Depends(verify_internal_token)],
)
async def generate_problem_endpoint(req: GenerateProblemRequest) -> GeneratedProblemSchema:
    try:
        return await generate_problem(req)
    except AIGenerationFailed as e:
        logger.exception("문제 생성 실패")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(e)) from e
