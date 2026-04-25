import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';

import { createCounterExamples, getCounterExamples } from '@/api/ai';

interface Props {
  submissionId: number;
}

export default function CounterExamplePanel({ submissionId }: Props) {
  const qc = useQueryClient();
  const queryKey = ['ai-counter-examples', submissionId];

  const { data, isFetching, error } = useQuery({
    queryKey,
    queryFn: () => getCounterExamples(submissionId),
    retry: false,
  });

  const create = useMutation({
    mutationFn: () => createCounterExamples(submissionId),
    onSuccess: (res) => qc.setQueryData(queryKey, res),
  });

  const notFound = (error as AxiosError | undefined)?.response?.status === 404;

  return (
    <section className="bg-white border rounded-md p-5">
      <header className="flex items-center justify-between mb-3">
        <div>
          <h2 className="text-lg font-semibold">AI 반례</h2>
          <p className="text-xs text-gray-500">놓치기 쉬운 엣지 케이스 입력을 보여드립니다.</p>
        </div>
        <button
          onClick={() => create.mutate()}
          disabled={create.isPending}
          className="px-3 py-1.5 rounded-md bg-rose-600 text-white text-sm font-medium hover:bg-rose-700 disabled:opacity-50"
        >
          {create.isPending ? '생성 중…' : data ? '재조회' : '반례 받기'}
        </button>
      </header>

      {isFetching && !data && <p className="text-sm text-gray-400">불러오는 중…</p>}
      {notFound && !data && !create.isPending && (
        <p className="text-sm text-gray-500">아직 이 제출에 대한 반례가 없습니다. ‘반례 받기’를 눌러 생성하세요.</p>
      )}
      {create.isError && (
        <p className="text-sm text-red-600">
          {(create.error as AxiosError<{ error?: { message?: string } }>)?.response?.data?.error?.message ??
            '반례 생성에 실패했습니다.'}
        </p>
      )}

      {data && data.items.length > 0 && (
        <ol className="space-y-3 text-sm">
          {data.items.map((c, i) => (
            <li key={c.id} className="border rounded-md p-3 bg-gray-50">
              <div className="flex items-baseline justify-between mb-2">
                <h3 className="font-medium">반례 #{i + 1}</h3>
                {c.relatedConstraint && (
                  <span className="text-xs text-gray-500">관련 제약: {c.relatedConstraint}</span>
                )}
              </div>
              <div className="grid md:grid-cols-2 gap-2">
                <div>
                  <div className="text-xs text-gray-500 mb-1">입력</div>
                  <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs whitespace-pre-wrap break-all">
                    {c.input}
                  </pre>
                </div>
                {c.expectedOutput && (
                  <div>
                    <div className="text-xs text-gray-500 mb-1">기대 출력</div>
                    <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs whitespace-pre-wrap break-all">
                      {c.expectedOutput}
                    </pre>
                  </div>
                )}
              </div>
              <div className="mt-2 text-gray-700">{c.reason}</div>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
