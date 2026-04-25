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

/** GET /api/problems 응답 요소 */
export interface ProblemSummary {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  aiGenerated: boolean;
  categories: string[];
  tags: string[];
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
