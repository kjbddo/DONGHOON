export type Difficulty = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND';

export type ProblemStatus = 'DRAFT' | 'PUBLIC' | 'PRIVATE' | 'DELETED' | 'REPORTED' | 'NEEDS_REVIEW';

export type SourceType =
  | 'ADMIN_CREATED'
  | 'AI_GENERATED'
  | 'USER_CREATED'
  | 'AI_REWRITTEN_SOURCE_BASED'
  | 'LICENSED_IMPORTED';

export interface Example {
  input: string;
  output: string;
  explanation?: string;
}

export interface PublicTestCase {
  seq: number;
  input: string;
  expectedOutput: string;
}

/** 로그인한 사용자 시점에서 본 문제 풀이 상태. */
export type ProblemUserStatus = 'SOLVED' | 'WRONG';

/** GET /api/problems 응답 요소 */
export interface ProblemSummary {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  aiGenerated: boolean;
  categories: string[];
  tags: string[];
  /** 비로그인이거나 시도 이력이 없으면 null. */
  userStatus?: ProblemUserStatus | null;
}

/** GET /api/problems/{id} 응답 */
export interface ProblemDetail {
  id: number;
  slug: string;
  title: string;
  description: string;
  inputDescription: string;
  outputDescription: string;
  constraints: string[];
  examples: Example[];
  timeLimitMs: number;
  memoryLimitMb: number;
  difficulty: Difficulty;
  aiGenerated: boolean;
  categories: string[];
  tags: string[];
  publicTestCases: PublicTestCase[];
}
