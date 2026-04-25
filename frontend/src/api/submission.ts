import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { SubmissionDetail, SubmissionSummary, SubmitRequest } from '@/types/submission';

export async function submitCode(req: SubmitRequest): Promise<SubmissionSummary> {
  const { data } = await apiClient.post<ApiResponse<SubmissionSummary>>('/submissions', req);
  return data.data;
}

export async function fetchMySubmissions(params: {
  page?: number;
  size?: number;
  problemId?: number;
} = {}): Promise<PageResponse<SubmissionSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<SubmissionSummary>>>('/submissions', {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      problemId: params.problemId,
    },
  });
  return data.data;
}

export async function fetchSubmission(id: number): Promise<SubmissionDetail> {
  const { data } = await apiClient.get<ApiResponse<SubmissionDetail>>(`/submissions/${id}`);
  return data.data;
}

import { useAuthStore } from '@/stores/authStore';

/**
 * 채점 결과 SSE 구독 (백엔드: GET /api/submissions/{id}/stream).
 *
 * 표준 EventSource는 Authorization 헤더를 못 넣으므로
 * fetch + ReadableStream 으로 SSE 프로토콜을 직접 파싱한다.
 *
 * 반환값: 구독을 취소하는 cleanup 함수.
 */
export function subscribeSubmissionStream(
  id: number,
  onMessage: (payload: unknown) => void,
  onError?: (err: unknown) => void
): () => void {
  const ctrl = new AbortController();
  const token = useAuthStore.getState().accessToken;

  (async () => {
    try {
      const res = await fetch(`/api/submissions/${id}/stream`, {
        headers: {
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        signal: ctrl.signal,
      });
      if (!res.ok || !res.body) {
        onError?.(new Error(`SSE 연결 실패: ${res.status}`));
        return;
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buf = '';
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true });
        let idx;
        while ((idx = buf.indexOf('\n\n')) !== -1) {
          const block = buf.slice(0, idx);
          buf = buf.slice(idx + 2);
          const line = block.split('\n').find((l) => l.startsWith('data:'));
          if (!line) continue;
          const data = line.slice(5).trim();
          if (!data) continue;
          try {
            onMessage(JSON.parse(data));
          } catch {
            onMessage(data);
          }
        }
      }
    } catch (e) {
      if ((e as { name?: string }).name !== 'AbortError') onError?.(e);
    }
  })();

  return () => ctrl.abort();
}
