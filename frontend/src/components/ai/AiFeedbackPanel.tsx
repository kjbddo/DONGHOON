import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';

import { createFeedback, getFeedback } from '@/api/ai';

const LEVELS = [
  { value: 1, label: 'Lv.1 약한 힌트' },
  { value: 2, label: 'Lv.2 방향 힌트' },
  { value: 3, label: 'Lv.3 반례·복잡도 힌트' },
  { value: 4, label: 'Lv.4 최대 힌트' },
];

interface Props {
  submissionId: number;
}

export default function AiFeedbackPanel({ submissionId }: Props) {
  const [level, setLevel] = useState(1);
  const qc = useQueryClient();

  const queryKey = ['ai-feedback', submissionId, level];

  const { data, isFetching, error } = useQuery({
    queryKey,
    queryFn: () => getFeedback(submissionId, level),
    retry: false,
  });

  const create = useMutation({
    mutationFn: () => createFeedback(submissionId, level),
    onSuccess: (res) => qc.setQueryData(queryKey, res),
  });

  const notFound = (error as AxiosError | undefined)?.response?.status === 404;

  return (
    <section className="bg-white border rounded-md p-5">
      <header className="flex items-center justify-between mb-3 gap-3">
        <div>
          <h2 className="text-lg font-semibold">AI 피드백</h2>
          <p className="text-xs text-gray-500">정답 코드는 알려드리지 않아요. 단계별 힌트를 제공합니다.</p>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={level}
            onChange={(e) => setLevel(Number(e.target.value))}
            className="border rounded-md px-2 py-1 text-sm"
          >
            {LEVELS.map((l) => (
              <option key={l.value} value={l.value}>
                {l.label}
              </option>
            ))}
          </select>
          <button
            onClick={() => create.mutate()}
            disabled={create.isPending}
            className="px-3 py-1.5 rounded-md bg-purple-600 text-white text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
          >
            {create.isPending ? '생성 중…' : data ? '재생성 (캐시됨)' : '힌트 받기'}
          </button>
        </div>
      </header>

      {isFetching && !data && <p className="text-sm text-gray-400">불러오는 중…</p>}
      {notFound && !data && !create.isPending && (
        <p className="text-sm text-gray-500">아직 이 레벨의 힌트가 없습니다. ‘힌트 받기’를 눌러 생성하세요.</p>
      )}
      {create.isError && (
        <p className="text-sm text-red-600">
          {(create.error as AxiosError<{ error?: { message?: string } }>)?.response?.data?.error?.message ??
            '힌트 생성에 실패했습니다.'}
        </p>
      )}

      {data && (
        <div className="space-y-3 text-sm">
          {data.summary && (
            <div>
              <h3 className="font-medium mb-1">요약</h3>
              <p className="text-gray-800 whitespace-pre-wrap">{data.summary}</p>
            </div>
          )}
          <HintRow label="방향 힌트" value={data.directionHint} />
          <HintRow label="반례 힌트" value={data.counterExampleHint} />
          <HintRow label="복잡도 힌트" value={data.complexityHint} />
          <HintRow label="런타임 에러 힌트" value={data.runtimeErrorHint} />
          <HintRow label="컴파일 에러 힌트" value={data.compileErrorHint} />
          <p className="text-xs text-gray-400">
            생성 시각: {new Date(data.createdAt).toLocaleString()}
          </p>
        </div>
      )}
    </section>
  );
}

function HintRow({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return (
    <div>
      <h3 className="font-medium mb-1">{label}</h3>
      <p className="text-gray-800 whitespace-pre-wrap">{value}</p>
    </div>
  );
}
