import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { generateAiProblem } from '@/api/admin';
import ProblemMarkdown from '@/components/markdown/ProblemMarkdown';
import type { AdminProblemDetail } from '@/types/admin';

const AI_CATEGORIES = [
  'DP',
  'GRAPH',
  'GREEDY',
  'STRING',
  'MATH',
  'DS',
  'BFS',
  'DFS',
  'BINARY_SEARCH',
  'TWO_POINTER',
  'BRUTE_FORCE',
  'SIMULATION',
  'BACKTRACKING',
  'SEGMENT_TREE',
  'TREE',
  'BIT',
  'GEOMETRY',
];

const CUSTOM_CATEGORY_VALUE = '__custom__';

const AI_DIFFICULTIES = ['Bronze', 'Silver', 'Gold', 'Platinum', 'Diamond', 'Ruby'];

export default function AdminAiProblemGeneratePage() {
  const [category, setCategory] = useState<string>('');
  const [customCategory, setCustomCategory] = useState<string>('');
  const [difficulty, setDifficulty] = useState<string>('');
  const [topicHint, setTopicHint] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [generated, setGenerated] = useState<AdminProblemDetail | null>(null);
  const qc = useQueryClient();

  const isCustom = category === CUSTOM_CATEGORY_VALUE;
  const resolvedCategory = isCustom ? customCategory.trim() : category;

  const mutation = useMutation({
    mutationFn: () =>
      generateAiProblem({
        category: resolvedCategory || undefined,
        difficulty: difficulty || undefined,
        topicHint: topicHint.trim() || undefined,
      }),
    onSuccess: (res) => {
      setGenerated(res);
      setErrorMsg(null);
      // 새 문제가 만들어졌으니 admin 리스트 / 사용자 리스트 / 단일문제 캐시 무효화.
      // (사용자가 다른 탭/페이지로 이동하자마자 자동으로 최신 목록을 받도록.)
      void qc.invalidateQueries({ queryKey: ['admin', 'problems'] });
      void qc.invalidateQueries({ queryKey: ['problems'] });
      void qc.invalidateQueries({ queryKey: ['admin', 'problem', res.id] });
    },
    onError: (err: unknown) => {
      const message = (err as { response?: { data?: { error?: { message?: string } } } })
        ?.response?.data?.error?.message;
      setErrorMsg(message ?? 'AI 문제 생성 중 오류가 발생했습니다.');
      setGenerated(null);
    },
  });

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold">AI 문제 생성</h1>
        <p className="text-sm text-gray-500 mt-1">
          Gemini 기반으로 새 문제를 생성하고 자동으로 <b>DRAFT</b> 상태로 등록합니다.
          이후 관리자가 검토 후 공개해야 합니다.
        </p>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate();
        }}
        className="bg-white border rounded p-4 space-y-3"
      >
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <label className="block">
            <span className="block text-xs text-gray-500 mb-1">카테고리</span>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm"
            >
              <option value="">자동 선택</option>
              {AI_CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
              <option value={CUSTOM_CATEGORY_VALUE}>직접 입력…</option>
            </select>
            {isCustom && (
              <input
                value={customCategory}
                onChange={(e) => setCustomCategory(e.target.value)}
                placeholder="예: NETWORK_FLOW, KMP, 게임이론"
                className="mt-2 w-full border rounded px-3 py-2 text-sm"
                maxLength={64}
              />
            )}
          </label>
          <label className="block">
            <span className="block text-xs text-gray-500 mb-1">난이도</span>
            <select
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm"
            >
              <option value="">자동 선택</option>
              {AI_DIFFICULTIES.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="block text-xs text-gray-500 mb-1">토픽 힌트 (옵션)</span>
            <input
              value={topicHint}
              onChange={(e) => setTopicHint(e.target.value)}
              placeholder="예: 트리 위에서의 LCA"
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </label>
        </div>

        <div className="flex items-center justify-end gap-2">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2 text-sm bg-purple-600 text-white rounded hover:bg-purple-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'AI 호출 중… (수십 초 소요)' : '문제 생성'}
          </button>
        </div>

        {errorMsg && (
          <div className="border border-red-300 bg-red-50 text-red-700 text-sm rounded p-3">
            {errorMsg}
          </div>
        )}
      </form>

      {generated && (
        <section className="bg-white border rounded p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-xs text-purple-600 font-semibold">AI 생성됨 · DRAFT</div>
              <h2 className="text-xl font-bold">{generated.title}</h2>
              <div className="text-xs text-gray-400">slug: {generated.slug}</div>
            </div>
            <Link
              to={`/admin/problems/${generated.id}`}
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              검토/편집하기
            </Link>
          </div>

          <div className="grid grid-cols-3 gap-3 text-sm">
            <Stat label="난이도" value={generated.difficulty} />
            <Stat label="시간 제한" value={`${generated.timeLimitMs} ms`} />
            <Stat label="메모리 제한" value={`${generated.memoryLimitMb} MB`} />
          </div>

          <Divider />
          <Subsection title="문제 설명">
            <ProblemMarkdown>{generated.description}</ProblemMarkdown>
          </Subsection>
          <Subsection title="입력 형식">
            <ProblemMarkdown>{generated.inputDescription}</ProblemMarkdown>
          </Subsection>
          <Subsection title="출력 형식">
            <ProblemMarkdown>{generated.outputDescription}</ProblemMarkdown>
          </Subsection>
          {generated.constraints.length > 0 && (
            <Subsection title="제약 조건">
              <ul className="list-disc ml-5 text-sm text-gray-800">
                {generated.constraints.map((c, i) => (
                  <li key={i}>
                    <ProblemMarkdown inline>{c}</ProblemMarkdown>
                  </li>
                ))}
              </ul>
            </Subsection>
          )}
          {generated.examples.length > 0 && (
            <Subsection title={`예제 (${generated.examples.length}개)`}>
              <div className="space-y-3">
                {generated.examples.map((ex, i) => (
                  <div key={i} className="border rounded p-2 text-xs">
                    <div className="grid grid-cols-2 gap-2 font-mono">
                      <div>
                        <div className="text-gray-500 mb-1">입력 #{i + 1}</div>
                        <pre className="whitespace-pre-wrap">{ex.input}</pre>
                      </div>
                      <div>
                        <div className="text-gray-500 mb-1">출력 #{i + 1}</div>
                        <pre className="whitespace-pre-wrap">{ex.output}</pre>
                      </div>
                    </div>
                    {ex.explanation && (
                      <div className="mt-2 text-gray-700">
                        <div className="text-gray-500 mb-1">설명</div>
                        <ProblemMarkdown>{ex.explanation}</ProblemMarkdown>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Subsection>
          )}
          <Subsection title={`테스트 케이스 (${generated.testCases.length}개)`}>
            <div className="space-y-2">
              {generated.testCases.map((tc) => (
                <div key={tc.seq} className="grid grid-cols-2 gap-2 border rounded p-2 text-xs font-mono">
                  <div>
                    <div className="text-gray-500 mb-1">
                      입력 #{tc.seq} {tc.hidden && <span className="text-red-500">(hidden)</span>}
                    </div>
                    <pre className="whitespace-pre-wrap">{tc.input}</pre>
                  </div>
                  <div>
                    <div className="text-gray-500 mb-1">출력</div>
                    <pre className="whitespace-pre-wrap">{tc.expectedOutput}</pre>
                  </div>
                </div>
              ))}
            </div>
          </Subsection>
        </section>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="border rounded p-3 bg-gray-50">
      <div className="text-xs text-gray-500">{label}</div>
      <div className="font-semibold">{value}</div>
    </div>
  );
}

function Subsection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="text-sm font-semibold text-gray-700 mb-1">{title}</div>
      {children}
    </div>
  );
}

function Divider() {
  return <div className="border-t" />;
}
