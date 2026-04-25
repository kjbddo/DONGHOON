import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';

import { fetchMySubmissions } from '@/api/submission';
import { fetchMyStats } from '@/api/user';
import { STATUS_COLOR, STATUS_LABEL } from '@/types/submission';
import type { Difficulty } from '@/types/problem';
import { useAuthStore } from '@/stores/authStore';

const DIFF_ORDER: Difficulty[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'];
const DIFF_KO: Record<Difficulty, string> = {
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  DIAMOND: '다이아',
};

export default function MyPage() {
  const { user } = useAuthStore();
  const { data: recent } = useQuery({
    queryKey: ['my-submissions', 0, 'mypage'],
    queryFn: () => fetchMySubmissions({ page: 0, size: 5 }),
  });
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['my-stats'],
    queryFn: () => fetchMyStats(),
  });

  const accepted = recent?.content?.filter((s) => s.status === 'ACCEPTED').length ?? 0;
  const total = recent?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">마이페이지</h1>

      <section className="bg-white border rounded-md p-5">
        <div className="text-sm text-gray-500">사용자</div>
        <div className="text-xl font-semibold">{user?.username ?? '-'}</div>
        <div className="text-sm text-gray-500">{user?.email ?? '-'}</div>
        {user?.roles && (
          <div className="mt-2 flex gap-1 flex-wrap">
            {user.roles.map((r) => (
              <span key={r} className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded">
                {r}
              </span>
            ))}
          </div>
        )}
        <div className="mt-4 flex flex-wrap gap-2 text-sm">
          <Link
            to="/mypage/solved"
            className="px-3 py-1.5 rounded-md border border-gray-300 text-gray-800 hover:bg-gray-50"
          >
            해결한 문제
          </Link>
          <Link
            to="/bookmarks"
            className="px-3 py-1.5 rounded-md border border-gray-300 text-gray-800 hover:bg-gray-50"
          >
            북마크
          </Link>
          <Link
            to="/ranking"
            className="px-3 py-1.5 rounded-md border border-gray-300 text-gray-800 hover:bg-gray-50"
          >
            랭킹
          </Link>
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold mb-3">활동 요약</h2>
        {statsLoading && <p className="text-sm text-gray-400">통계를 불러오는 중…</p>}
        {stats && (
          <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-3">
            <Stat label="해결" value={String(stats.solvedCount)} />
            <Stat label="시도 (문제)" value={String(stats.attemptedCount)} />
            <Stat label="총 제출" value={String(stats.totalSubmissions)} />
            <Stat
              label="랭크"
              value={stats.rank > 0 ? String(stats.rank) : '-'}
            />
            <Stat
              label="정답률(제출)"
              value={stats.totalSubmissions > 0 ? `${(stats.acceptanceRate * 100).toFixed(1)}%` : '0%'}
            />
            <Stat label="북마크" value={String(stats.bookmarkCount)} />
          </div>
        )}
      </section>

      {stats && (
        <section className="bg-white border rounded-md p-5">
          <h2 className="text-lg font-semibold mb-3">난이도별 해결</h2>
          <div className="grid sm:grid-cols-2 gap-2 text-sm">
            {DIFF_ORDER.map((d) => {
              const n = stats.solvedByDifficulty[d] ?? 0;
              if (n === 0) return null;
              return (
                <div key={d} className="flex justify-between">
                  <span className="text-gray-600">{DIFF_KO[d]}</span>
                  <span className="font-medium tabular-nums">{n}</span>
                </div>
              );
            })}
            {DIFF_ORDER.every((d) => !stats.solvedByDifficulty[d]) && (
              <p className="text-gray-400 col-span-2">아직 맞힌 문제가 없습니다.</p>
            )}
          </div>
        </section>
      )}

      <section className="grid md:grid-cols-3 gap-3">
        <Stat label="(최근5) 총 제출 수" value={String(total)} />
        <Stat label="(최근5) 정답" value={`${accepted} / ${recent?.content?.length ?? 0}`} />
        <Stat label="권한" value={user?.roles?.includes('ROLE_ADMIN') ? '관리자' : '일반'} />
      </section>

      <section className="bg-white border rounded-md p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold">최근 제출</h2>
          <Link to="/submissions" className="text-sm text-blue-600 hover:underline">
            전체 보기
          </Link>
        </div>
        {!recent || recent.content.length === 0 ? (
          <p className="text-sm text-gray-400">아직 제출이 없습니다.</p>
        ) : (
          <ul className="divide-y text-sm">
            {recent.content.map((s) => (
              <li key={s.id} className="py-2 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Link to={`/submissions/${s.id}`} className="text-blue-700 hover:underline">
                    #{s.id}
                  </Link>
                  <Link to={`/problems/${s.problemId}`} className="text-gray-700 hover:underline">
                    문제 #{s.problemId}
                  </Link>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[s.status]}`}>
                    {STATUS_LABEL[s.status]}
                  </span>
                  <span className="text-xs text-gray-500">
                    {new Date(s.submittedAt).toLocaleString()}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white border rounded-md p-4">
      <div className="text-xs text-gray-500">{label}</div>
      <div className="text-xl font-semibold mt-1 tabular-nums">{value}</div>
    </div>
  );
}
