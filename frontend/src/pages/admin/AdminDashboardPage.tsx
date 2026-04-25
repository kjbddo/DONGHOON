import { Link } from 'react-router-dom';
import { useQueries } from '@tanstack/react-query';

import { fetchAdminProblems } from '@/api/admin';

interface Card {
  label: string;
  loading: boolean;
  value: number | string;
  color: string;
  to?: string;
}

export default function AdminDashboardPage() {
  // 4개 카드 각각 size=1로 totalElements만 활용 (가벼운 카운트 조회)
  const queries = useQueries({
    queries: [
      {
        queryKey: ['admin', 'count', 'all'],
        queryFn: () => fetchAdminProblems({ size: 1, includeDeleted: true }),
      },
      {
        queryKey: ['admin', 'count', 'public'],
        queryFn: () => fetchAdminProblems({ size: 1, status: 'PUBLIC' }),
      },
      {
        queryKey: ['admin', 'count', 'draft'],
        queryFn: () => fetchAdminProblems({ size: 1, status: 'DRAFT' }),
      },
      {
        queryKey: ['admin', 'count', 'ai'],
        queryFn: () => fetchAdminProblems({ size: 1, ai: true, includeDeleted: true }),
      },
      {
        queryKey: ['admin', 'count', 'reported'],
        queryFn: () => fetchAdminProblems({ size: 1, status: 'REPORTED' }),
      },
    ],
  });

  const [allQ, publicQ, draftQ, aiQ, reportedQ] = queries;

  const cards: Card[] = [
    {
      label: '전체 문제',
      value: allQ.data?.totalElements ?? '-',
      loading: allQ.isLoading,
      color: 'text-gray-900',
      to: '/admin/problems',
    },
    {
      label: '공개 문제',
      value: publicQ.data?.totalElements ?? '-',
      loading: publicQ.isLoading,
      color: 'text-emerald-600',
      to: '/admin/problems',
    },
    {
      label: '검토 대기 (DRAFT)',
      value: draftQ.data?.totalElements ?? '-',
      loading: draftQ.isLoading,
      color: 'text-blue-600',
      to: '/admin/problems',
    },
    {
      label: 'AI 생성 문제',
      value: aiQ.data?.totalElements ?? '-',
      loading: aiQ.isLoading,
      color: 'text-purple-600',
      to: '/admin/problems',
    },
    {
      label: '신고된 문제',
      value: reportedQ.data?.totalElements ?? '-',
      loading: reportedQ.isLoading,
      color: 'text-orange-600',
      to: '/admin/problems',
    },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">관리자 대시보드</h1>
        <p className="text-sm text-gray-500 mt-1">
          AlgoForge 운영 현황을 한눈에 확인하고, 자주 쓰는 작업으로 빠르게 이동하세요.
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {cards.map((c) => (
          <Link
            key={c.label}
            to={c.to ?? '#'}
            className="bg-white border rounded p-4 hover:shadow-sm transition"
          >
            <div className="text-xs text-gray-500">{c.label}</div>
            <div className={`text-3xl font-bold mt-1 ${c.color}`}>
              {c.loading ? '…' : c.value}
            </div>
          </Link>
        ))}
      </div>

      <section className="bg-white border rounded p-5">
        <h2 className="font-semibold text-gray-700 mb-3">빠른 작업</h2>
        <div className="flex flex-wrap gap-2">
          <Link
            to="/admin/problems/new"
            className="px-4 py-2 text-sm rounded bg-blue-600 text-white hover:bg-blue-700"
          >
            + 문제 작성
          </Link>
          <Link
            to="/admin/problems/ai-generate"
            className="px-4 py-2 text-sm rounded bg-purple-600 text-white hover:bg-purple-700"
          >
            ✨ AI로 문제 생성
          </Link>
          <Link
            to="/admin/problems"
            className="px-4 py-2 text-sm rounded border hover:bg-gray-50"
          >
            문제 목록 보기
          </Link>
          <Link
            to="/admin/problem-imports"
            className="px-4 py-2 text-sm rounded border hover:bg-gray-50"
          >
            문제 불러오기
          </Link>
        </div>
      </section>
    </div>
  );
}
