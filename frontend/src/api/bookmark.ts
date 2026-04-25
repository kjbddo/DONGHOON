import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { ProblemSummary } from '@/types/problem';

export async function fetchBookmarks(
  page = 0,
  size = 20,
): Promise<PageResponse<ProblemSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<ProblemSummary>>>(
    '/users/me/bookmarks',
    { params: { page, size } },
  );
  return data.data;
}

export async function addBookmark(problemId: number): Promise<boolean> {
  const { data } = await apiClient.post<ApiResponse<boolean>>(
    `/users/me/bookmarks/${problemId}`,
  );
  return data.data;
}

export async function removeBookmark(problemId: number): Promise<boolean> {
  const { data } = await apiClient.delete<ApiResponse<boolean>>(
    `/users/me/bookmarks/${problemId}`,
  );
  return data.data;
}

export async function isBookmarked(problemId: number): Promise<boolean> {
  const { data } = await apiClient.get<ApiResponse<boolean>>(
    `/users/me/bookmarks/${problemId}/exists`,
  );
  return data.data;
}
