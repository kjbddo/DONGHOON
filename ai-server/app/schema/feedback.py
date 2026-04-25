from typing import Optional

from pydantic import BaseModel, Field


class FeedbackRequest(BaseModel):
    problem_title: str = Field(alias="problemTitle")
    description: str
    input_description: str = Field(alias="inputDescription")
    output_description: str = Field(alias="outputDescription")
    constraints: list[str]
    user_code: str = Field(alias="userCode")
    language: str
    judge_status: str = Field(alias="judgeStatus")
    feedback_level: int = Field(alias="feedbackLevel", ge=1, le=4)
    failed_test_excerpt: Optional[str] = Field(default=None, alias="failedTestExcerpt")
    runtime_error_message: Optional[str] = Field(default=None, alias="runtimeErrorMessage")
    compile_error_message: Optional[str] = Field(default=None, alias="compileErrorMessage")

    class Config:
        populate_by_name = True


class FeedbackResponseSchema(BaseModel):
    feedbackLevel: int = Field(ge=1, le=4)
    summary: str
    directionHint: Optional[str] = None
    counterExampleHint: Optional[str] = None
    complexityHint: Optional[str] = None
    runtimeErrorHint: Optional[str] = None
    compileErrorHint: Optional[str] = None
    shouldRevealAnswerCode: bool = False
