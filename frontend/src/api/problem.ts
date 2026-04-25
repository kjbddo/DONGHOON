import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { ProblemDetail, ProblemSummary } from '@/types/problem';

export interface ProblemListParams {
  page?: number;
  size?: number;
  difficulty?: string;
  category?: string;
  keyword?: string;
}

export async function fetchProblems(params: ProblemListParams = {}): Promise<PageResponse<ProblemSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<ProblemSummary>>>('/problems', {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      difficulty: params.difficulty || undefined,
      category: params.category || undefined,
      keyword: params.keyword || undefined,
    },
  });
  return data.data;
}

export async function fetchProblem(id: number): Promise<ProblemDetail> {
  const { data } = await apiClient.get<ApiResponse<ProblemDetail>>(`/problems/${id}`);
  return data.data;
}
