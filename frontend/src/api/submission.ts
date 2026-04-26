import { apiClient } from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type {
  SubmissionDetail,
  SubmissionStatus,
  SubmissionSummary,
  SubmitRequest,
} from '@/types/submission';

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

const FINAL_STATES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILE_ERROR',
  'RUNTIME_ERROR',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'SYSTEM_ERROR',
];

const isFinal = (s?: SubmissionStatus | null) => !!s && FINAL_STATES.includes(s);

/**
 * 채점 결과 SSE 구독 (백엔드: GET /api/submissions/{id}/stream).
 *
 * 표준 EventSource는 Authorization 헤더를 못 넣으므로
 * fetch + ReadableStream 으로 SSE 프로토콜을 직접 파싱한다.
 *
 * SSE 가 막히는 환경(프록시/회사망/오프라인 잠깐 등)을 대비해
 * SSE 연결이 실패하거나 final 상태 도달 전 조기 종료되면
 * 1.5s 간격으로 GET /api/submissions/{id} 폴링 fallback 으로 전환한다.
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
  let pollTimer: ReturnType<typeof setTimeout> | null = null;
  let stopped = false;
  let lastStatus: SubmissionStatus | null = null;

  const stop = () => {
    stopped = true;
    if (pollTimer) clearTimeout(pollTimer);
    pollTimer = null;
    ctrl.abort();
  };

  const handle = (status?: SubmissionStatus) => {
    if (!status || stopped) return;
    if (status !== lastStatus) {
      lastStatus = status;
      onMessage({ status });
    }
    if (isFinal(status)) stop();
  };

  // ---- 폴링 fallback ----------------------------------------------------
  const startPolling = () => {
    if (stopped || pollTimer) return;
    const tick = async () => {
      if (stopped) return;
      try {
        const detail = await fetchSubmission(id);
        handle(detail.status);
      } catch (e) {
        // 일시적 네트워크 오류는 무시하고 다음 tick 에서 재시도
        if (stopped) return;
        onError?.(e);
      }
      if (!stopped) pollTimer = setTimeout(tick, 1500);
    };
    pollTimer = setTimeout(tick, 0);
  };

  // ---- SSE 본체 ---------------------------------------------------------
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
        // SSE 자체가 실패 → 폴링으로 전환
        startPolling();
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
            const obj = JSON.parse(data) as { status?: SubmissionStatus };
            onMessage(obj);
            handle(obj.status);
          } catch {
            onMessage(data);
          }
        }
      }
      // SSE 가 final 도달 전에 자연 종료된 경우 → 폴링으로 마무리
      if (!stopped && !isFinal(lastStatus)) startPolling();
    } catch (e) {
      if ((e as { name?: string }).name === 'AbortError') return;
      // 네트워크 오류 등 → 폴링 fallback
      onError?.(e);
      startPolling();
    }
  })();

  return stop;
}
