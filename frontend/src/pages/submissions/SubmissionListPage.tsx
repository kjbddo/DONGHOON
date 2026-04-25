import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import { fetchMySubmissions } from '@/api/submission';
import { STATUS_COLOR, STATUS_LABEL } from '@/types/submission';

export default function SubmissionListPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['my-submissions', page],
    queryFn: () => fetchMySubmissions({ page, size: 20 }),
    placeholderData: keepPreviousData,
    refetchInterval: 5_000, // 채점 중인 항목 자동 갱신
  });

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">내 제출</h1>
      <div className="bg-white border rounded-md overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr className="text-left">
              <th className="px-3 py-2 w-20">#</th>
              <th className="px-3 py-2 w-24">문제</th>
              <th className="px-3 py-2 w-44">상태</th>
              <th className="px-3 py-2 w-24">시간</th>
              <th className="px-3 py-2 w-24">메모리</th>
              <th className="px-3 py-2">제출 시각</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr>
                <td colSpan={6} className="text-center py-10 text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            )}
            {isError && (
              <tr>
                <td colSpan={6} className="text-center py-10 text-red-500">
                  목록을 불러오지 못했습니다.
                </td>
              </tr>
            )}
            {data?.content?.map((s) => (
              <tr key={s.id} className="border-t hover:bg-gray-50">
                <td className="px-3 py-2 text-gray-500">
                  <Link to={`/submissions/${s.id}`} className="text-blue-700 hover:underline">
                    {s.id}
                  </Link>
                </td>
                <td className="px-3 py-2">
                  <Link to={`/problems/${s.problemId}`} className="text-gray-700 hover:underline">
                    #{s.problemId}
                  </Link>
                </td>
                <td className="px-3 py-2">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[s.status]}`}>
                    {STATUS_LABEL[s.status]}
                  </span>
                </td>
                <td className="px-3 py-2 text-gray-700">{s.executionTimeMs ? `${s.executionTimeMs} ms` : '-'}</td>
                <td className="px-3 py-2 text-gray-700">{s.memoryUsedKb ? `${s.memoryUsedKb} KB` : '-'}</td>
                <td className="px-3 py-2 text-gray-500">{new Date(s.submittedAt).toLocaleString()}</td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center py-10 text-gray-400">
                  아직 제출이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

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
