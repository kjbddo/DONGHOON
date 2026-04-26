import { Link, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { addBookmark, isBookmarked, removeBookmark } from '@/api/bookmark';
import { fetchProblem } from '@/api/problem';
import ProblemMarkdown from '@/components/markdown/ProblemMarkdown';
import AiBadge from '@/components/problem/AiBadge';
import DifficultyBadge from '@/components/problem/DifficultyBadge';
import { useAuthStore } from '@/stores/authStore';

export default function ProblemDetailPage() {
  const { id } = useParams();
  const problemId = Number(id);
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['problem', problemId],
    queryFn: () => fetchProblem(problemId),
    enabled: !!problemId,
  });

  const { data: bookmarked } = useQuery({
    queryKey: ['bookmark', problemId],
    queryFn: () => isBookmarked(problemId),
    enabled: !!user && !!problemId,
  });

  const toggleBookmark = useMutation({
    mutationFn: async () => {
      if (bookmarked) {
        return removeBookmark(problemId);
      }
      return addBookmark(problemId);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['bookmark', problemId] });
      void queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
      void queryClient.invalidateQueries({ queryKey: ['my-stats'] });
    },
  });

  if (isLoading) return <div className="text-gray-400 py-10 text-center">불러오는 중…</div>;
  if (isError || !data) return <div className="text-red-500 py-10 text-center">문제를 불러오지 못했습니다.</div>;

  return (
    <div className="space-y-6">
      <header className="bg-white border rounded-md p-5">
        <div className="flex items-center gap-2 flex-wrap mb-2">
          <DifficultyBadge difficulty={data.difficulty} />
          {data.aiGenerated && <AiBadge />}
          {data.categories.map((c) => (
            <span key={c} className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded">
              {c}
            </span>
          ))}
        </div>
        <h1 className="text-2xl font-bold">
          #{data.id} {data.title}
        </h1>
        <div className="mt-2 text-sm text-gray-500 flex gap-4">
          <span>시간 제한: {data.timeLimitMs} ms</span>
          <span>메모리 제한: {data.memoryLimitMb} MB</span>
        </div>
        <div className="mt-4 flex flex-wrap gap-2 items-center">
          {user ? (
            <Link
              to={`/problems/${data.id}/solve`}
              className="px-4 py-2 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700"
            >
              문제 풀기
            </Link>
          ) : (
            <Link
              to={`/login?redirect=/problems/${data.id}/solve`}
              className="px-4 py-2 rounded-md border border-blue-600 text-blue-700 text-sm font-medium hover:bg-blue-50"
            >
              로그인하고 풀기
            </Link>
          )}
          {user && (
            <button
              type="button"
              onClick={() => toggleBookmark.mutate()}
              disabled={toggleBookmark.isPending}
              className={`px-4 py-2 rounded-md text-sm font-medium border ${
                bookmarked
                  ? 'border-amber-500 bg-amber-50 text-amber-900 hover:bg-amber-100'
                  : 'border-gray-300 text-gray-800 hover:bg-gray-100'
              } disabled:opacity-50`}
            >
              {bookmarked ? '북마크됨' : '북마크'}
            </button>
          )}
        </div>
      </header>

      <Section title="문제 설명">
        <ProblemMarkdown>{data.description}</ProblemMarkdown>
      </Section>

      <Section title="입력">
        <ProblemMarkdown>{data.inputDescription}</ProblemMarkdown>
      </Section>

      <Section title="출력">
        <ProblemMarkdown>{data.outputDescription}</ProblemMarkdown>
      </Section>

      {data.constraints.length > 0 && (
        <Section title="제약 사항">
          <ul className="list-disc pl-5 space-y-1 text-sm">
            {data.constraints.map((c, i) => (
              <li key={i}>
                <ProblemMarkdown inline>{c}</ProblemMarkdown>
              </li>
            ))}
          </ul>
        </Section>
      )}

      {data.examples.length > 0 && (
        <Section title="예제">
          <div className="space-y-3">
            {data.examples.map((ex, i) => (
              <div key={i} className="grid md:grid-cols-2 gap-3">
                <Code label={`예제 입력 ${i + 1}`} value={ex.input} />
                <Code label={`예제 출력 ${i + 1}`} value={ex.output} />
                {ex.explanation && (
                  <div className="md:col-span-2 text-sm text-gray-600">
                    <ProblemMarkdown>{ex.explanation}</ProblemMarkdown>
                  </div>
                )}
              </div>
            ))}
          </div>
        </Section>
      )}

      {data.publicTestCases.length > 0 && (
        <Section title="공개 테스트 케이스">
          <div className="space-y-3">
            {data.publicTestCases.map((tc) => (
              <div key={tc.seq} className="grid md:grid-cols-2 gap-3">
                <Code label={`Test #${tc.seq} 입력`} value={tc.input} />
                <Code label={`Test #${tc.seq} 출력`} value={tc.expectedOutput} />
              </div>
            ))}
          </div>
        </Section>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="bg-white border rounded-md p-5">
      <h2 className="text-lg font-semibold mb-3">{title}</h2>
      {children}
    </section>
  );
}

function Code({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      <pre className="bg-gray-900 text-gray-100 rounded-md p-3 text-xs whitespace-pre-wrap break-all">
        {value}
      </pre>
    </div>
  );
}
