import type { Difficulty } from '@/types/problem';

const COLOR: Record<Difficulty, string> = {
  BRONZE: 'bg-amber-100 text-amber-800',
  SILVER: 'bg-gray-200 text-gray-800',
  GOLD: 'bg-yellow-100 text-yellow-800',
  PLATINUM: 'bg-cyan-100 text-cyan-800',
  DIAMOND: 'bg-blue-100 text-blue-800',
};

const LABEL: Record<Difficulty, string> = {
  BRONZE: 'Bronze',
  SILVER: 'Silver',
  GOLD: 'Gold',
  PLATINUM: 'Platinum',
  DIAMOND: 'Diamond',
};

export default function DifficultyBadge({ difficulty }: { difficulty: Difficulty }) {
  return (
    <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${COLOR[difficulty] ?? 'bg-gray-100 text-gray-700'}`}>
      {LABEL[difficulty] ?? difficulty}
    </span>
  );
}
