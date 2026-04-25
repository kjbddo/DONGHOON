import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';

import { fetchProblem } from '@/api/problem';
import { submitCode, subscribeSubmissionStream } from '@/api/submission';
import CodeEditor from '@/components/editor/CodeEditor';
import DifficultyBadge from '@/components/problem/DifficultyBadge';
import {
  STATUS_COLOR,
  STATUS_LABEL,
  type SubmissionStatus,
} from '@/types/submission';

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

  // 제출 + SSE 상태
  const [submissionId, setSubmissionId] = useState<number | null>(null);
  const [status, setStatus] = useState<SubmissionStatus | null>(null);
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
    setStatus('PENDING');
    try {
      const res = await submitCode({ problemId: problem.id, language, code });
      setSubmissionId(res.id);
      setStatus(res.status);
    } catch (e) {
      const ax = e as AxiosError<{ error?: { message?: string } }>;
      setError(ax.response?.data?.error?.message ?? '제출에 실패했습니다.');
      setStatus(null);
    } finally {
      setSubmitting(false);
    }
  };

  // SSE 구독
  useEffect(() => {
    if (!submissionId) return;
    const cleanup = subscribeSubmissionStream(
      submissionId,
      (payload) => {
        const obj = payload as { status?: SubmissionStatus };
        if (obj?.status) setStatus(obj.status);
      },
      () => {
        // 연결 끊김 시에도 detail 페이지에서 다시 조회 가능
      }
    );
    return cleanup;
  }, [submissionId]);

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
        <article className="prose prose-sm max-w-none whitespace-pre-wrap">
          <h3>문제</h3>
          <p>{problem.description}</p>
          <h3>입력</h3>
          <p>{problem.inputDescription}</p>
          <h3>출력</h3>
          <p>{problem.outputDescription}</p>
          {problem.constraints.length > 0 && (
            <>
              <h3>제약</h3>
              <ul>
                {problem.constraints.map((c, i) => (
                  <li key={i}>{c}</li>
                ))}
              </ul>
            </>
          )}
          {problem.examples.length > 0 && (
            <>
              <h3>예제</h3>
              {problem.examples.map((ex, i) => (
                <div key={i} className="mb-3">
                  <div className="text-xs text-gray-500">예제 입력 {i + 1}</div>
                  <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs">{ex.input}</pre>
                  <div className="text-xs text-gray-500 mt-1">예제 출력 {i + 1}</div>
                  <pre className="bg-gray-900 text-gray-100 rounded p-2 text-xs">{ex.output}</pre>
                </div>
              ))}
            </>
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
            {status && (
              <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[status]}`}>
                {STATUS_LABEL[status]}
              </span>
            )}
            <button
              onClick={onSubmit}
              disabled={submitting}
              className="px-4 py-1.5 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? '제출 중…' : '제출'}
            </button>
            {submissionId && status && (status === 'ACCEPTED' || status === 'WRONG_ANSWER' || (status as string).endsWith('ERROR') || (status as string).endsWith('EXCEEDED')) && (
              <button
                onClick={() => navigate(`/submissions/${submissionId}`)}
                className="px-3 py-1.5 rounded-md border text-sm hover:bg-gray-50"
              >
                결과 상세
              </button>
            )}
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
