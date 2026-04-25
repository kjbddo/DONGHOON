import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { ProblemSummary } from '@/types/problem';
import type { RankingEntry, UserStats } from '@/types/user';

export async function fetchMyStats(): Promise<UserStats> {
  const { data } = await apiClient.get<ApiResponse<UserStats>>('/users/me/stats');
  return data.data;
}

export async function fetchUserStats(userId: number): Promise<UserStats> {
  const { data } = await apiClient.get<ApiResponse<UserStats>>(`/users/${userId}/stats`);
  return data.data;
}

export async function fetchMySolvedProblems(
  page = 0,
  size = 20,
): Promise<PageResponse<ProblemSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<ProblemSummary>>>(
    '/users/me/solved',
    { params: { page, size } },
  );
  return data.data;
}

export async function fetchRanking(
  page = 0,
  size = 50,
): Promise<PageResponse<RankingEntry>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<RankingEntry>>>(
    '/users/ranking',
    { params: { page, size } },
  );
  return data.data;
}
