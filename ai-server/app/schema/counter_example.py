from typing import List, Optional

from pydantic import BaseModel, Field


class CounterExampleRequest(BaseModel):
    problem_title: str = Field(alias="problemTitle")
    description: str
    constraints: list[str]
    user_code: str = Field(alias="userCode")
    language: str
    failed_test_excerpt: Optional[str] = Field(default=None, alias="failedTestExcerpt")

    class Config:
        populate_by_name = True


class CounterExampleItem(BaseModel):
    input: str
    expectedOutput: Optional[str] = None
    reason: str
    relatedConstraint: Optional[str] = None


class CounterExampleResponseSchema(BaseModel):
    counterExamples: List[CounterExampleItem] = Field(min_length=1, max_length=10)
