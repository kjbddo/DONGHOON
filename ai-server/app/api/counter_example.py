import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.chains.counter_example_chain import generate_counter_examples
from app.core.auth import verify_internal_token
from app.core.retry import AIGenerationFailed
from app.schema.counter_example import (
    CounterExampleRequest,
    CounterExampleResponseSchema,
)

router = APIRouter(prefix="/ai/counter-examples", tags=["AI CounterExample"])
logger = logging.getLogger(__name__)


@router.post(
    "",
    response_model=CounterExampleResponseSchema,
    dependencies=[Depends(verify_internal_token)],
)
async def counter_example_endpoint(req: CounterExampleRequest) -> CounterExampleResponseSchema:
    try:
        return await generate_counter_examples(req)
    except AIGenerationFailed as e:
        logger.exception("반례 생성 실패")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(e)) from e
