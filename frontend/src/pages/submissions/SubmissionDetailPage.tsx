import { useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';

import { fetchSubmission, subscribeSubmissionStream } from '@/api/submission';
import AiFeedbackPanel from '@/components/ai/AiFeedbackPanel';
import CounterExamplePanel from '@/components/ai/CounterExamplePanel';
import { STATUS_COLOR, STATUS_LABEL, type SubmissionDetail, type SubmissionStatus } from '@/types/submission';

const FINAL_STATES: SubmissionStatus[] = [
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILE_ERROR',
  'RUNTIME_ERROR',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'SYSTEM_ERROR',
];

function judgedToast(status: SubmissionStatus) {
  const label = STATUS_LABEL[status];
  if (status === 'ACCEPTED') {
    toast.success(`채점 완료: ${label}`, { icon: '🎉' });
  } else {
    toast.error(`채점 완료: ${label}`);
  }
}

export default function SubmissionDetailPage() {
  const { id } = useParams();
  const submissionId = Number(id);
  const qc = useQueryClient();

  const queryKey = ['submission', submissionId];
  const { data, isLoading, isError } = useQuery({
    queryKey,
    queryFn: () => fetchSubmission(submissionId),
    enabled: !!submissionId,
  });

  // 한 제출에 대해 토스트는 한 번만 띄운다 (페이지 재방문/포커스 변경 시 중복 방지)
  const toastedRef = useRef<number | null>(null);

  // 처음 화면을 열었을 때 이미 final 이면(=새로고침/재진입) 토스트는 생략하고 캐시만 표시
  useEffect(() => {
    if (data && FINAL_STATES.includes(data.status)) {
      toastedRef.current = submissionId;
    }
  }, [data, submissionId]);

  // 채점 진행 중이면 SSE(+폴링 fallback)로 갱신, final 진입 시 토스트
  useEffect(() => {
    if (!data) return;
    if (FINAL_STATES.includes(data.status)) return;

    const cleanup = subscribeSubmissionStream(submissionId, (payload) => {
      const obj = payload as { status?: SubmissionStatus };
      if (!obj?.status) return;

      qc.setQueryData<SubmissionDetail>(queryKey, (old) => (old ? { ...old, status: obj.status! } : old));

      if (FINAL_STATES.includes(obj.status)) {
        qc.invalidateQueries({ queryKey });
        if (toastedRef.current !== submissionId) {
          toastedRef.current = submissionId;
          judgedToast(obj.status);
        }
      }
    });
    return cleanup;
  }, [data, submissionId, qc]);

  if (isLoading) return <div className="text-gray-400 py-10 text-center">불러오는 중…</div>;
  if (isError || !data) return <div className="text-red-500 py-10 text-center">제출을 불러오지 못했습니다.</div>;

  const showAi = data.status !== 'ACCEPTED' && data.status !== 'PENDING' && data.status !== 'JUDGING';

  return (
    <div className="space-y-5">
      <header className="bg-white border rounded-md p-5">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div>
            <div className="text-sm text-gray-500 mb-1">
              제출 #{data.id} ·{' '}
              <Link to={`/problems/${data.problemId}`} className="hover:underline">
                문제 #{data.problemId}
              </Link>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`px-2 py-0.5 rounded text-sm font-medium ${STATUS_COLOR[data.status]}`}>
                {STATUS_LABEL[data.status]}
              </span>
              {data.executionTimeMs !== undefined && (
                <span className="text-sm text-gray-500">{data.executionTimeMs} ms</span>
              )}
              {data.memoryUsedKb !== undefined && (
                <span className="text-sm text-gray-500">{data.memoryUsedKb} KB</span>
              )}
            </div>
          </div>
          <div className="text-right text-xs text-gray-500">
            <div>제출: {new Date(data.submittedAt).toLocaleString()}</div>
            {data.judgedAt && <div>채점: {new Date(data.judgedAt).toLocaleString()}</div>}
          </div>
        </div>
      </header>

      {data.compileErrorMessage && (
        <ErrorBox title="컴파일 에러" message={data.compileErrorMessage} />
      )}
      {data.runtimeErrorMessage && (
        <ErrorBox title="런타임 에러" message={data.runtimeErrorMessage} />
      )}

      <section className="bg-white border rounded-md p-5">
        <h2 className="text-lg font-semibold mb-3">테스트 케이스 결과</h2>
        {data.testCaseResults.length === 0 ? (
          <p className="text-sm text-gray-400">결과가 아직 없습니다.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr className="text-left">
                <th className="px-3 py-2 w-16">#</th>
                <th className="px-3 py-2 w-44">상태</th>
                <th className="px-3 py-2 w-24">시간</th>
                <th className="px-3 py-2 w-24">메모리</th>
                <th className="px-3 py-2">출력 발췌</th>
              </tr>
            </thead>
            <tbody>
              {data.testCaseResults.map((r, idx) => (
                <tr key={r.testCaseId} className="border-t">
                  <td className="px-3 py-2 text-gray-500">{idx + 1}</td>
                  <td className="px-3 py-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[r.status]}`}>
                      {STATUS_LABEL[r.status]}
                    </span>
                  </td>
                  <td className="px-3 py-2">{r.executionTimeMs ? `${r.executionTimeMs} ms` : '-'}</td>
                  <td className="px-3 py-2">{r.memoryUsedKb ? `${r.memoryUsedKb} KB` : '-'}</td>
                  <td className="px-3 py-2 font-mono text-xs text-gray-700 truncate max-w-md">
                    {r.outputExcerpt ?? '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="bg-white border rounded-md p-5">
        <h2 className="text-lg font-semibold mb-3">제출 코드</h2>
        <pre className="bg-gray-900 text-gray-100 rounded-md p-4 text-xs overflow-auto whitespace-pre-wrap break-all max-h-96">
          {data.code}
        </pre>
      </section>

      {showAi && (
        <>
          <AiFeedbackPanel submissionId={data.id} />
          <CounterExamplePanel submissionId={data.id} />
        </>
      )}
    </div>
  );
}

function ErrorBox({ title, message }: { title: string; message: string }) {
  return (
    <section className="bg-white border-l-4 border-red-500 rounded-md p-4">
      <h3 className="text-sm font-semibold text-red-700 mb-1">{title}</h3>
      <pre className="text-xs text-red-800 whitespace-pre-wrap break-all">{message}</pre>
    </section>
  );
}
