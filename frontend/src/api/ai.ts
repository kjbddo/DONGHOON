import { apiClient } from './client';
import type { ApiResponse } from '@/types/common';
import type { AiFeedback, CounterExamples } from '@/types/ai';

// AI 호출은 LLM 응답 대기 + 백엔드 검증 재시도까지 합쳐 최대 ~3분 걸릴 수 있다.
// apiClient 의 기본 timeout(15s) 으로는 LLM 호출이 끝나기 전에 axios 가 먼저 끊어
// "AI 호출 실패" 토스트가 뜨고, 백엔드는 그 사이에 결과를 캐싱한다.
// (원인: ai_call_logs 의 latency_ms 가 17~25s 인 케이스에서 UI 만 실패하던 버그)
const AI_TIMEOUT_MS = 180_000;

export async function getFeedback(submissionId: number, level: number): Promise<AiFeedback> {
  const { data } = await apiClient.get<ApiResponse<AiFeedback>>(
    `/submissions/${submissionId}/feedback`,
    { params: { level }, timeout: AI_TIMEOUT_MS }
  );
  return data.data;
}

export async function createFeedback(submissionId: number, level: number): Promise<AiFeedback> {
  const { data } = await apiClient.post<ApiResponse<AiFeedback>>(
    `/submissions/${submissionId}/feedback`,
    null,
    { params: { level }, timeout: AI_TIMEOUT_MS }
  );
  return data.data;
}

export async function getCounterExamples(submissionId: number): Promise<CounterExamples> {
  const { data } = await apiClient.get<ApiResponse<CounterExamples>>(
    `/submissions/${submissionId}/counter-examples`,
    { timeout: AI_TIMEOUT_MS }
  );
  return data.data;
}

export async function createCounterExamples(submissionId: number): Promise<CounterExamples> {
  const { data } = await apiClient.post<ApiResponse<CounterExamples>>(
    `/submissions/${submissionId}/counter-examples`,
    null,
    { timeout: AI_TIMEOUT_MS }
  );
  return data.data;
}
