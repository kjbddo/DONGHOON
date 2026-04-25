import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';

import { importAdminProblem } from '@/api/admin';
import type { ImportMode, ProblemImportRequestBody } from '@/types/admin';

const SAMPLE_JSON = `{
  "mode": "METADATA_ONLY",
  "payload": {
    "title": "가져온 샘플 문제",
    "slug": "",
    "description": "## 설명\n두 수의 합을 출력합니다.",
    "inputDescription": "한 줄에 두 정수",
    "outputDescription": "한 줄에 합",
    "constraints": ["0 ≤ a,b ≤ 10^9"],
    "examples": [{ "input": "1 2", "output": "3" }],
    "timeLimitMs": 2000,
    "memoryLimitMb": 256,
    "difficulty": "BRONZE",
    "categories": ["수학"],
    "tags": ["import"],
    "testCases": [
      { "seq": 0, "input": "1 2", "expectedOutput": "3", "hidden": false }
    ]
  }
}`;

export default function AdminProblemImportPage() {
  const [text, setText] = useState(SAMPLE_JSON);
  const [err, setErr] = useState<string | null>(null);
  const [resultId, setResultId] = useState<number | null>(null);

  const m = useMutation({
    mutationFn: (body: ProblemImportRequestBody) => importAdminProblem(body),
    onSuccess: (d) => {
      setResultId(d.id);
      setErr(null);
    },
    onError: (e: unknown) => {
      setResultId(null);
      const msg =
        typeof e === 'object' && e !== null && 'response' in e
          ? (e as { response?: { data?: { error?: { message?: string } } } }).response?.data?.error
              ?.message
          : null;
      setErr(msg ?? (e instanceof Error ? e.message : '요청 실패'));
    },
  });

  return (
    <div className="max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold">문제 가져오기 (JSON)</h1>
      <p className="text-sm text-gray-600">
        <code>AdminProblemCreateRequest</code>와 동일한 <code>payload</code>를 쓰되,{' '}
        <code>sourceType</code>은 무시되고 <code>mode</code>에 따라 서버가 결정합니다.{' '}
        <code>LICENSED_IMPORT</code>는 동의 + (설정 시) 출처 URL 도메인이 허용 목록에 있어야
        합니다.
      </p>

      <div className="space-y-2">
        <label className="text-sm font-medium">요청 JSON</label>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={22}
          className="w-full font-mono text-sm border rounded-md p-3"
          spellCheck={false}
        />
      </div>

      <div className="flex flex-wrap gap-2 items-center">
        <button
          type="button"
          onClick={() => {
            setErr(null);
            setResultId(null);
            let parsed: unknown;
            try {
              parsed = JSON.parse(text) as unknown;
            } catch {
              setErr('JSON 파싱에 실패했습니다.');
              return;
            }
            if (typeof parsed !== 'object' || parsed === null || !('mode' in parsed) || !('payload' in parsed)) {
              setErr('최상위에 mode, payload가 필요합니다.');
              return;
            }
            const o = parsed as { mode: ImportMode; payload: unknown };
            if (o.mode === 'LICENSED_IMPORT' && !('licenseAcknowledged' in (parsed as object))) {
              setErr('LICENSED_IMPORT 는 licenseAcknowledged: true 가 필요합니다.');
              return;
            }
            m.mutate(parsed as ProblemImportRequestBody);
          }}
          disabled={m.isPending}
          className="px-4 py-2 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
        >
          {m.isPending ? '가져오는 중…' : 'DRAFT로 등록'}
        </button>
        <button
          type="button"
          className="text-sm text-gray-600 hover:underline"
          onClick={() => setText(SAMPLE_JSON)}
        >
          샘플로 초기화
        </button>
        <Link to="/admin/problems" className="text-sm text-blue-600 hover:underline">
          문제 목록
        </Link>
      </div>

      {err && <p className="text-sm text-red-600">{err}</p>}

      {resultId !== null && (
        <p className="text-sm text-emerald-700">
          등록됨. 문제 ID:{' '}
          <Link to={`/admin/problems/${resultId}`} className="font-mono font-semibold underline">
            {resultId}
          </Link>
        </p>
      )}

      <section className="text-xs text-gray-500 space-y-1 border-t pt-4">
        <p>
          <strong>METADATA_ONLY</strong> → <code>ADMIN_CREATED</code>
        </p>
        <p>
          <strong>LICENSED_IMPORT</strong> → <code>LICENSED_IMPORTED</code> (licenseAcknowledged 필수)
        </p>
        <p>
          <strong>AI_REWRITE_FROM_METADATA</strong> → <code>AI_REWRITTEN_SOURCE_BASED</code> (본문은 payload
          그대로, 이후 수동/AI 다듬기)
        </p>
      </section>
    </div>
  );
}
