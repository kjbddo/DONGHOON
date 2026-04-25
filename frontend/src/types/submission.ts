export type SubmissionStatus =
  | 'PENDING'
  | 'JUDGING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILE_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'SYSTEM_ERROR';

export interface SubmissionSummary {
  id: number;
  problemId: number;
  languageId: number;
  status: SubmissionStatus;
  executionTimeMs?: number;
  memoryUsedKb?: number;
  submittedAt: string;
  judgedAt?: string;
}

export interface TestCaseResult {
  testCaseId: number;
  status: SubmissionStatus;
  executionTimeMs?: number;
  memoryUsedKb?: number;
  outputExcerpt?: string;
}

export interface SubmissionDetail extends SubmissionSummary {
  code: string;
  compileErrorMessage?: string;
  runtimeErrorMessage?: string;
  testCaseResults: TestCaseResult[];
}

export interface SubmitRequest {
  problemId: number;
  language: string; // CodeLanguage.name (ex: "JAVA", "PYTHON")
  code: string;
}

export const STATUS_LABEL: Record<SubmissionStatus, string> = {
  PENDING: '대기',
  JUDGING: '채점 중',
  ACCEPTED: '맞았습니다!',
  WRONG_ANSWER: '틀렸습니다',
  COMPILE_ERROR: '컴파일 에러',
  RUNTIME_ERROR: '런타임 에러',
  TIME_LIMIT_EXCEEDED: '시간 초과',
  MEMORY_LIMIT_EXCEEDED: '메모리 초과',
  SYSTEM_ERROR: '시스템 오류',
};

export const STATUS_COLOR: Record<SubmissionStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-700',
  JUDGING: 'bg-blue-100 text-blue-700 animate-pulse',
  ACCEPTED: 'bg-green-100 text-green-800',
  WRONG_ANSWER: 'bg-red-100 text-red-800',
  COMPILE_ERROR: 'bg-orange-100 text-orange-800',
  RUNTIME_ERROR: 'bg-orange-100 text-orange-800',
  TIME_LIMIT_EXCEEDED: 'bg-yellow-100 text-yellow-800',
  MEMORY_LIMIT_EXCEEDED: 'bg-yellow-100 text-yellow-800',
  SYSTEM_ERROR: 'bg-red-100 text-red-800',
};
