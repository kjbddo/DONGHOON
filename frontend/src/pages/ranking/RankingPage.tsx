import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { fetchRanking } from '@/api/user';

const PAGE_SIZE = 50;

export default function RankingPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['ranking', page],
    queryFn: () => fetchRanking(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">랭킹</h1>
      <p className="text-sm text-gray-500">
        정답(Accepted)으로 맞힌 서로 다른 문제 수를 기준으로 합니다. 푼 문제가 없으면 표시되지 않습니다.
      </p>

      <div className="bg-white border rounded-md overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr className="text-left">
              <th className="px-3 py-2 w-20">순위</th>
              <th className="px-3 py-2">사용자</th>
              <th className="px-3 py-2 w-28 text-right">해결</th>
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
                  아직 랭킹 데이터가 없습니다.
                </td>
              </tr>
            )}
            {data?.content.map((row) => (
              <tr key={row.userId} className="border-t">
                <td className="px-3 py-2 font-mono text-gray-700">{row.rank}</td>
                <td className="px-3 py-2">
                  <Link
                    to={`/users/${row.userId}`}
                    className="text-blue-700 hover:underline font-medium"
                  >
                    {row.username}
                  </Link>
                </td>
                <td className="px-3 py-2 text-right tabular-nums">{row.solvedCount}</td>
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
            onClick={() => setPage((p) => Math.max(0, p - 1))}
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
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 rounded border border-gray-300 disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
