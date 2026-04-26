import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import toast from 'react-hot-toast';

import { fetchProblem } from '@/api/problem';
import { submitCode } from '@/api/submission';
import CodeEditor from '@/components/editor/CodeEditor';
import ProblemMarkdown from '@/components/markdown/ProblemMarkdown';
import DifficultyBadge from '@/components/problem/DifficultyBadge';

const LANGUAGES = [
  { value: 'JAVA', label: 'Java' },
  { value: 'PYTHON', label: 'Python' },
  { value: 'CPP', label: 'C++' },
  { value: 'JAVASCRIPT', label: 'JavaScript' },
];

const TEMPLATES: Record<string, string> = {
  JAVA: `import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // TODO\n    }\n}\n`,
  PYTHON: `import sys\ninput = sys.stdin.readline\n\n# TODO\n`,
  CPP: `#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n    // TODO\n    return 0;\n}\n`,
  JAVASCRIPT: `const lines = require('fs').readFileSync(0, 'utf8').trim().split('\\n');\n// TODO\n`,
};

export default function ProblemSolvePage() {
  const { id } = useParams();
  const problemId = Number(id);
  const navigate = useNavigate();

  const { data: problem } = useQuery({
    queryKey: ['problem', problemId],
    queryFn: () => fetchProblem(problemId),
    enabled: !!problemId,
  });

  const [language, setLanguage] = useState('JAVA');
  const storageKey = useMemo(() => `algoforge.solve.${problemId}.${language}`, [problemId, language]);
  const [code, setCode] = useState<string>('');

  // 언어별 임시 저장 코드 로드
  useEffect(() => {
    const saved = localStorage.getItem(storageKey);
    setCode(saved ?? TEMPLATES[language] ?? '');
  }, [storageKey, language]);

  useEffect(() => {
    if (!code) return;
    const t = setTimeout(() => localStorage.setItem(storageKey, code), 500);
    return () => clearTimeout(t);
  }, [code, storageKey]);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async () => {
    if (!problem) return;
    if (code.trim().length === 0) {
      setError('코드를 입력하세요.');
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const res = await submitCode({ problemId: problem.id, language, code });
      toast.success(`제출 #${res.id} 등록됨 · 채점 진행 중…`);
      // 제출되면 곧바로 디테일 페이지로 이동.
      // 거기서 SSE/폴링으로 채점 상태가 자동 갱신되고, 완료 시 토스트가 뜬다.
      navigate(`/submissions/${res.id}`);
    } catch (e) {
      const ax = e as AxiosError<{ error?: { message?: string } }>;
      const msg = ax.response?.data?.error?.message ?? '제출에 실패했습니다.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (!problem) return <div className="text-gray-400 py-10 text-center">불러오는 중…</div>;

  return (
    <div className="grid lg:grid-cols-2 gap-4 h-[calc(100vh-8rem)]">
      {/* 좌측: 문제 요약 */}
      <div className="bg-white border rounded-md p-5 overflow-auto">
        <div className="flex items-center gap-2 mb-2">
          <DifficultyBadge difficulty={problem.difficulty} />
          <span className="text-xs text-gray-500">
            {problem.timeLimitMs} ms · {problem.memoryLimitMb} MB
          </span>
        </div>
        <h1 className="text-xl font-bold mb-3">
          #{problem.id} {problem.title}
        </h1>
        <article className="space-y-4">
          <section>
            <h3 className="text-base font-semibold mb-1">문제</h3>
            <ProblemMarkdown>{problem.description}</ProblemMarkdown>
          </section>
          <section>
            <h3 className="text-base font-semibold mb-1">입력</h3>
            <ProblemMarkdown>{problem.inputDescription}</ProblemMarkdown>
          </section>
          <section>
            <h3 className="text-base font-semibold mb-1">출력</h3>
            <ProblemMarkdown>{problem.outputDescription}</ProblemMarkdown>
          </section>
          {problem.constraints.length > 0 && (
            <section>
              <h3 className="text-base font-semibold mb-1">제약</h3>
              <ul className="list-disc pl-5 space-y-1 text-sm">
                {problem.constraints.map((c, i) => (
                  <li key={i}>
                    <ProblemMarkdown inline>{c}</ProblemMarkdown>
                  </li>
                ))}
              </ul>
            </section>
          )}
          {problem.examples.length > 0 && (
            <section>
              <h3 className="text-base font-semibold mb-1">예제</h3>
              {problem.examples.map((ex, i) => (
                <div key={i} className="mb-3">
                  <div className="text-xs text-gray-500">예제 입력 {i + 1}</div>
                  <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs whitespace-pre-wrap">{ex.input}</pre>
                  <div className="text-xs text-gray-500 mt-1">예제 출력 {i + 1}</div>
                  <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs whitespace-pre-wrap">{ex.output}</pre>
                  {ex.explanation && (
                    <div className="mt-1 text-xs text-gray-700">
                      <ProblemMarkdown>{ex.explanation}</ProblemMarkdown>
                    </div>
                  )}
                </div>
              ))}
            </section>
          )}
        </article>
      </div>

      {/* 우측: 에디터 */}
      <div className="flex flex-col bg-white border rounded-md overflow-hidden">
        <div className="flex items-center justify-between p-3 border-b">
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="border rounded-md px-2 py-1 text-sm"
          >
            {LANGUAGES.map((l) => (
              <option key={l.value} value={l.value}>
                {l.label}
              </option>
            ))}
          </select>
          <div className="flex gap-2 items-center">
            <button
              onClick={onSubmit}
              disabled={submitting}
              className="px-4 py-1.5 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? '제출 중…' : '제출'}
            </button>
          </div>
        </div>
        <div className="flex-1">
          <CodeEditor value={code} onChange={setCode} language={language} height="100%" />
        </div>
        {error && <div className="p-3 text-sm text-red-600 border-t">{error}</div>}
      </div>
    </div>
  );
}
