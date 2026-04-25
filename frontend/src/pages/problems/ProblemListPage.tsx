import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { fetchProblems } from '@/api/problem';
import AiBadge from '@/components/problem/AiBadge';
import DifficultyBadge from '@/components/problem/DifficultyBadge';
import type { Difficulty } from '@/types/problem';

const DIFFICULTIES: Difficulty[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'];

export default function ProblemListPage() {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [difficulty, setDifficulty] = useState<string>('');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['problems', { page, keyword, difficulty }],
    queryFn: () => fetchProblems({ page, size: 20, keyword: keyword || undefined, difficulty: difficulty || undefined }),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">문제 목록</h1>

      {/* 필터 */}
      <form
        className="flex flex-wrap gap-2 mb-4 items-center"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          setKeyword(keywordInput.trim());
        }}
      >
        <input
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="제목/태그 검색"
          className="border rounded-md px-3 py-1.5 text-sm w-64"
        />
        <select
          value={difficulty}
          onChange={(e) => {
            setPage(0);
            setDifficulty(e.target.value);
          }}
          className="border rounded-md px-2 py-1.5 text-sm"
        >
          <option value="">난이도 전체</option>
          {DIFFICULTIES.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
        <button type="submit" className="px-3 py-1.5 rounded-md bg-blue-600 text-white text-sm hover:bg-blue-700">
          검색
        </button>
      </form>

      <div className="bg-white border rounded-md overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr className="text-left">
              <th className="px-3 py-2 w-16">#</th>
              <th className="px-3 py-2">제목</th>
              <th className="px-3 py-2 w-32">난이도</th>
              <th className="px-3 py-2">카테고리</th>
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
            {data?.content?.map((p) => (
              <tr key={p.id} className="border-t hover:bg-gray-50">
                <td className="px-3 py-2 text-gray-500">{p.id}</td>
                <td className="px-3 py-2">
                  <Link to={`/problems/${p.id}`} className="text-blue-700 hover:underline font-medium">
                    {p.title}
                  </Link>
                  {p.aiGenerated && (
                    <span className="ml-2 align-middle">
                      <AiBadge />
                    </span>
                  )}
                </td>
                <td className="px-3 py-2">
                  <DifficultyBadge difficulty={p.difficulty} />
                </td>
                <td className="px-3 py-2 text-gray-600">{p.categories?.join(', ') || '-'}</td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={4} className="text-center py-10 text-gray-400">
                  결과가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 페이지네이션 */}
      {data && (
        <div className="flex items-center justify-center gap-2 mt-4 text-sm">
          <button
            disabled={data.first}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1 border rounded-md disabled:opacity-30"
          >
            이전
          </button>
          <span className="px-2 text-gray-500">
            {data.number + 1} / {Math.max(1, data.totalPages)}
          </span>
          <button
            disabled={data.last}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 border rounded-md disabled:opacity-30"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
