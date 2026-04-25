import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type {
  AdminProblemDetail,
  AdminProblemListParams,
  AdminProblemSummary,
  AdminProblemUpsertPayload,
  AiGenerateRequest,
  ProblemImportRequestBody,
} from '@/types/admin';
import type { ProblemStatus } from '@/types/problem';

export async function fetchAdminProblems(
  params: AdminProblemListParams = {}
): Promise<PageResponse<AdminProblemSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<AdminProblemSummary>>>(
    '/admin/problems',
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        status: params.status || undefined,
        difficulty: params.difficulty || undefined,
        ai: params.ai === undefined ? undefined : params.ai,
        includeDeleted: params.includeDeleted ?? false,
        keyword: params.keyword || undefined,
      },
    }
  );
  return data.data;
}

export async function fetchAdminProblem(id: number): Promise<AdminProblemDetail> {
  const { data } = await apiClient.get<ApiResponse<AdminProblemDetail>>(`/admin/problems/${id}`);
  return data.data;
}

export async function createAdminProblem(payload: AdminProblemUpsertPayload): Promise<AdminProblemDetail> {
  const { data } = await apiClient.post<ApiResponse<AdminProblemDetail>>('/admin/problems', payload);
  return data.data;
}

export async function updateAdminProblem(
  id: number,
  payload: AdminProblemUpsertPayload
): Promise<AdminProblemDetail> {
  const { data } = await apiClient.put<ApiResponse<AdminProblemDetail>>(`/admin/problems/${id}`, payload);
  return data.data;
}

export async function changeProblemStatus(id: number, status: ProblemStatus): Promise<AdminProblemDetail> {
  const { data } = await apiClient.patch<ApiResponse<AdminProblemDetail>>(
    `/admin/problems/${id}/status`,
    { status }
  );
  return data.data;
}

export async function deleteAdminProblem(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(`/admin/problems/${id}`);
}

export async function generateAiProblem(payload: AiGenerateRequest = {}): Promise<AdminProblemDetail> {
  const { data } = await apiClient.post<ApiResponse<AdminProblemDetail>>(
    '/admin/problems/ai/generate',
    payload
  );
  return data.data;
}

export async function importAdminProblem(body: ProblemImportRequestBody): Promise<AdminProblemDetail> {
  const { data } = await apiClient.post<ApiResponse<AdminProblemDetail>>('/admin/problems/import', body);
  return data.data;
}
