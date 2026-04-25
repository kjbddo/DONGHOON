import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { fetchUserStats } from '@/api/user';
import { STATUS_LABEL, type SubmissionStatus } from '@/types/submission';
import type { Difficulty } from '@/types/problem';

const DIFF_ORDER: Difficulty[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'];

const DIFF_KO: Record<Difficulty, string> = {
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  DIAMOND: '다이아',
};

export default function UserProfilePage() {
  const { userId: rawId } = useParams();
  const userId = Number(rawId);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['user-stats', userId],
    queryFn: () => fetchUserStats(userId),
    enabled: Number.isFinite(userId) && userId > 0,
  });

  if (!Number.isFinite(userId) || userId <= 0) {
    return <p className="text-red-500">잘못된 사용자 ID입니다.</p>;
  }

  if (isLoading) return <div className="text-gray-400 py-10">불러오는 중…</div>;
  if (isError || !data) return <div className="text-red-500 py-10">프로필을 불러오지 못했습니다.</div>;

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <Link to="/ranking" className="text-sm text-blue-600 hover:underline">
          ← 랭킹
        </Link>
        <h1 className="text-2xl font-bold mt-2">{data.username}</h1>
        <p className="text-sm text-gray-500">공개 통계</p>
      </div>

      <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-3">
        <StatBox label="해결" value={String(data.solvedCount)} sub="다른 문제를 맞힌 횟수" />
        <StatBox label="시도" value={String(data.attemptedCount)} sub="1회 이상 제출" />
        <StatBox
          label="랭크"
          value={data.rank > 0 ? String(data.rank) : '-'}
          sub="푼 수 기준"
        />
        <StatBox
          label="정답률(제출 대비)"
          value={`${(data.acceptanceRate * 100).toFixed(1)}%`}
          sub={`총 제출 ${data.totalSubmissions}`}
        />
        <StatBox label="북마크" value={String(data.bookmarkCount)} sub="북마크한 문제" />
      </div>

      <section className="bg-white border rounded-md p-5">
        <h2 className="text-lg font-semibold mb-3">난이도별 해결</h2>
        <ul className="text-sm space-y-1">
          {DIFF_ORDER.map((d) => {
            const n = data.solvedByDifficulty[d] ?? 0;
            if (n === 0) return null;
            return (
              <li key={d} className="flex justify-between">
                <span>{DIFF_KO[d]}</span>
                <span className="tabular-nums text-gray-700">{n}</span>
              </li>
            );
          })}
          {DIFF_ORDER.every((d) => !data.solvedByDifficulty[d]) && (
            <li className="text-gray-400">아직 난이도별 기록이 없습니다.</li>
          )}
        </ul>
      </section>

      <section className="bg-white border rounded-md p-5">
        <h2 className="text-lg font-semibold mb-3">제출 상태 분포</h2>
        <ul className="text-sm space-y-1">
          {Object.entries(data.submissionsByStatus).map(([status, n]) => (
            <li key={status} className="flex justify-between">
              <span>{STATUS_LABEL[status as SubmissionStatus] ?? status}</span>
              <span className="tabular-nums text-gray-700">{n}</span>
            </li>
          ))}
          {Object.keys(data.submissionsByStatus).length === 0 && (
            <li className="text-gray-400">제출이 없습니다.</li>
          )}
        </ul>
      </section>

      <section className="bg-white border rounded-md p-5">
        <h2 className="text-lg font-semibold mb-3">언어 사용 (제출 수, 상위)</h2>
        <ul className="text-sm space-y-1">
          {Object.entries(data.languageUsage).map(([langId, n]) => (
            <li key={langId} className="flex justify-between">
              <span>언어 ID {langId}</span>
              <span className="tabular-nums text-gray-700">{n}</span>
            </li>
          ))}
          {Object.keys(data.languageUsage).length === 0 && (
            <li className="text-gray-400">기록이 없습니다.</li>
          )}
        </ul>
      </section>
    </div>
  );
}

function StatBox({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div className="bg-white border rounded-md p-4">
      <div className="text-xs text-gray-500">{label}</div>
      <div className="text-xl font-semibold mt-1 tabular-nums">{value}</div>
      <div className="text-xs text-gray-400 mt-0.5">{sub}</div>
    </div>
  );
}
