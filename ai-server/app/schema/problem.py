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


CategoryLiteral = Literal[
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
    category: CategoryLiteral
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
    category: Optional[CategoryLiteral] = None
    difficulty: Optional[DifficultyLiteral] = None
    topic_hint: Optional[str] = Field(default=None, alias="topicHint")
    source_metadata: Optional[dict] = Field(default=None, alias="sourceMetadata")

    class Config:
        populate_by_name = True
