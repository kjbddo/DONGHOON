import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { fetchMySolvedProblems } from '@/api/user';
import AiBadge from '@/components/problem/AiBadge';
import DifficultyBadge from '@/components/problem/DifficultyBadge';

const PAGE_SIZE = 20;

export default function SolvedProblemsPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['my-solved', page],
    queryFn: () => fetchMySolvedProblems(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">해결한 문제</h1>
        <p className="text-sm text-gray-500">정답으로 맞힌 문제만 표시됩니다 (최근 해결 순).</p>
      </div>
      <Link to="/mypage" className="text-sm text-blue-600 hover:underline">
        ← 마이페이지
      </Link>

      <div className="bg-white border rounded-md overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr className="text-left">
              <th className="px-3 py-2 w-16">#</th>
              <th className="px-3 py-2">제목</th>
              <th className="px-3 py-2 w-32">난이도</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr>
                <td colSpan={3} className="text-center py-10 text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            )}
            {isError && (
              <tr>
                <td colSpan={3} className="text-center py-10 text-red-500">
                  목록을 불러오지 못했습니다.
                </td>
              </tr>
            )}
            {data && data.content.length === 0 && !isLoading && (
              <tr>
                <td colSpan={3} className="text-center py-10 text-gray-400">
                  아직 맞힌 문제가 없습니다.
                </td>
              </tr>
            )}
            {data?.content.map((p) => (
              <tr key={p.id} className="border-t">
                <td className="px-3 py-2 text-gray-500">{p.id}</td>
                <td className="px-3 py-2">
                  <div className="flex items-center gap-2">
                    <Link to={`/problems/${p.id}`} className="text-blue-700 hover:underline">
                      {p.title}
                    </Link>
                    {p.aiGenerated && <AiBadge />}
                  </div>
                </td>
                <td className="px-3 py-2">
                  <DifficultyBadge difficulty={p.difficulty} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 text-sm">
          <button
            type="button"
            disabled={data.first}
            onClick={() => setPage((x) => Math.max(0, x - 1))}
            className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-gray-500">
            {data.number + 1} / {data.totalPages}
          </span>
          <button
            type="button"
            disabled={data.last}
            onClick={() => setPage((x) => x + 1)}
            className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
