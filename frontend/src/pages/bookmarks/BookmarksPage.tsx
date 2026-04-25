import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';

import { removeBookmark, fetchBookmarks } from '@/api/bookmark';
import AiBadge from '@/components/problem/AiBadge';
import DifficultyBadge from '@/components/problem/DifficultyBadge';

const PAGE_SIZE = 20;

export default function BookmarksPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['bookmarks', page],
    queryFn: () => fetchBookmarks(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const unbookmark = useMutation({
    mutationFn: (problemId: number) => removeBookmark(problemId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
    },
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">북마크</h1>
      <p className="text-sm text-gray-500">공개된 문제만 북마크할 수 있습니다.</p>

      <div className="bg-white border rounded-md overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr className="text-left">
              <th className="px-3 py-2 w-16">#</th>
              <th className="px-3 py-2">제목</th>
              <th className="px-3 py-2 w-32">난이도</th>
              <th className="px-3 py-2 w-24 text-right">관리</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr>
                <td colSpan={4} className="text-center py-10 text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            )}
            {isError && (
              <tr>
                <td colSpan={4} className="text-center py-10 text-red-500">
                  목록을 불러오지 못했습니다.
                </td>
              </tr>
            )}
            {data && data.content.length === 0 && !isLoading && (
              <tr>
                <td colSpan={4} className="text-center py-10 text-gray-400">
                  북마크한 문제가 없습니다. 문제 상세에서 북마크를 추가하세요.
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
                <td className="px-3 py-2 text-right">
                  <button
                    type="button"
                    onClick={() => unbookmark.mutate(p.id)}
                    disabled={unbookmark.isPending}
                    className="text-xs text-red-600 hover:underline disabled:opacity-50"
                  >
                    해제
                  </button>
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
