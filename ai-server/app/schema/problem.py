from typing import List, Literal, Optional

from pydantic import BaseModel, Field


class ExampleSchema(BaseModel):
    input: str
    output: str
    explanation: Optional[str] = None


class ImageSchema(BaseModel):
    url: str
    description: Optional[str] = None


class TestCaseSchema(BaseModel):
    input: str
    output: str
    isHidden: bool


class SolutionCodeSchema(BaseModel):
    language: Literal["Java", "Python", "Cpp", "JavaScript"]
    code: str


# 권장 enum 카테고리. 관리자가 직접 입력하면 자유 문자열로 받아 그대로 통과시킨다.
SUGGESTED_CATEGORIES: List[str] = [
    "DP",
    "GRAPH",
    "GREEDY",
    "STRING",
    "MATH",
    "DS",
    "BFS",
    "DFS",
    "BINARY_SEARCH",
    "TWO_POINTER",
    "BRUTE_FORCE",
    "SIMULATION",
    "BACKTRACKING",
    "SEGMENT_TREE",
    "TREE",
    "BIT",
    "GEOMETRY",
]

DifficultyLiteral = Literal["Bronze", "Silver", "Gold", "Platinum", "Diamond", "Ruby"]


class GeneratedProblemSchema(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    # 관리자 직접 입력 카테고리 지원을 위해 자유 문자열로 둔다 (백엔드에서 find-or-create).
    category: str = Field(min_length=1, max_length=64)
    difficulty: DifficultyLiteral
    description: str = Field(min_length=20)
    inputDescription: str
    outputDescription: str
    constraints: List[str] = Field(min_length=1)
    examples: List[ExampleSchema] = Field(min_length=1, max_length=5)
    images: Optional[List[ImageSchema]] = []
    testCases: List[TestCaseSchema] = Field(min_length=3, max_length=30)
    solutionOutline: str
    officialSolutionCode: SolutionCodeSchema
    timeLimit: int = Field(ge=1, le=10)
    memoryLimit: int = Field(ge=64, le=1024)
    sourceType: Literal["AI_GENERATED", "AI_REWRITTEN_SOURCE_BASED"]
    isAiGenerated: bool


class GenerateProblemRequest(BaseModel):
    # 관리자 직접 입력 카테고리 지원: 자유 문자열 허용. 비어있으면 LLM이 적절히 선택한다.
    category: Optional[str] = Field(default=None, max_length=64)
    difficulty: Optional[DifficultyLiteral] = None
    topic_hint: Optional[str] = Field(default=None, alias="topicHint")
    source_metadata: Optional[dict] = Field(default=None, alias="sourceMetadata")

    class Config:
        populate_by_name = True
