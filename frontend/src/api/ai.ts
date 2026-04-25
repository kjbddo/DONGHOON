import { apiClient } from './client';
import type { ApiResponse } from '@/types/common';
import type { AiFeedback, CounterExamples } from '@/types/ai';

export async function getFeedback(submissionId: number, level: number): Promise<AiFeedback> {
  const { data } = await apiClient.get<ApiResponse<AiFeedback>>(
    `/submissions/${submissionId}/feedback`,
    { params: { level } }
  );
  return data.data;
}

export async function createFeedback(submissionId: number, level: number): Promise<AiFeedback> {
  const { data } = await apiClient.post<ApiResponse<AiFeedback>>(
    `/submissions/${submissionId}/feedback`,
    null,
    { params: { level } }
  );
  return data.data;
}

export async function getCounterExamples(submissionId: number): Promise<CounterExamples> {
  const { data } = await apiClient.get<ApiResponse<CounterExamples>>(
    `/submissions/${submissionId}/counter-examples`
  );
  return data.data;
}

export async function createCounterExamples(submissionId: number): Promise<CounterExamples> {
  const { data } = await apiClient.post<ApiResponse<CounterExamples>>(
    `/submissions/${submissionId}/counter-examples`
  );
  return data.data;
}
