import type { Difficulty, Example, ProblemStatus, SourceType } from './problem';

export const DIFFICULTY_OPTIONS: Difficulty[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'];

export const PROBLEM_STATUS_OPTIONS: ProblemStatus[] = [
  'DRAFT',
  'PUBLIC',
  'PRIVATE',
  'NEEDS_REVIEW',
  'REPORTED',
  'DELETED',
];

export const SOURCE_TYPE_OPTIONS: SourceType[] = [
  'ADMIN_CREATED',
  'AI_GENERATED',
  'AI_REWRITTEN_SOURCE_BASED',
  'LICENSED_IMPORTED',
  'USER_CREATED',
];

export interface AdminProblemSummary {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  status: ProblemStatus;
  sourceType: SourceType;
  aiGenerated: boolean;
  qualityScore: number | null;
  reportCount: number;
  createdAt: string;
}

export interface AdminTestCase {
  id?: number;
  seq: number;
  input: string;
  expectedOutput: string;
  hidden: boolean;
}

export interface AdminProblemDetail {
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
  status: ProblemStatus;
  sourceType: SourceType;
  aiGenerated: boolean;
  aiModelName: string | null;
  aiPromptVersion: string | null;
  generatedByUserId: number | null;
  qualityScore: number | null;
  reportCount: number;
  generatedAt: string | null;
  createdAt: string;
  updatedAt: string;
  categories: string[];
  tags: string[];
  testCases: AdminTestCase[];
}

export interface AdminProblemUpsertPayload {
  title: string;
  slug?: string;
  description: string;
  inputDescription: string;
  outputDescription: string;
  constraints: string[];
  examples: Example[];
  timeLimitMs: number;
  memoryLimitMb: number;
  difficulty: Difficulty;
  sourceType?: SourceType;
  categories: string[];
  tags: string[];
  testCases: AdminTestCase[];
}

export interface AiGenerateRequest {
  category?: string;
  difficulty?: string;
  topicHint?: string;
}

export type ImportMode = 'METADATA_ONLY' | 'LICENSED_IMPORT' | 'AI_REWRITE_FROM_METADATA';

/** POST /api/admin/problems/import — payload.sourceType은 서버가 mode로 덮어씀 */
export interface ProblemImportRequestBody {
  mode: ImportMode;
  payload: AdminProblemUpsertPayload;
  licenseAcknowledged?: boolean;
  sourceUrl?: string;
  sourceSite?: string;
}

export interface AdminProblemListParams {
  page?: number;
  size?: number;
  status?: ProblemStatus;
  difficulty?: Difficulty;
  ai?: boolean;
  includeDeleted?: boolean;
  keyword?: string;
}

export const STATUS_COLOR: Record<ProblemStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700 border-gray-300',
  PUBLIC: 'bg-emerald-100 text-emerald-700 border-emerald-300',
  PRIVATE: 'bg-blue-100 text-blue-700 border-blue-300',
  NEEDS_REVIEW: 'bg-amber-100 text-amber-700 border-amber-300',
  REPORTED: 'bg-orange-100 text-orange-700 border-orange-300',
  DELETED: 'bg-red-100 text-red-700 border-red-300',
};

export const STATUS_LABEL: Record<ProblemStatus, string> = {
  DRAFT: '초안',
  PUBLIC: '공개',
  PRIVATE: '비공개',
  NEEDS_REVIEW: '검토 필요',
  REPORTED: '신고됨',
  DELETED: '삭제됨',
};
