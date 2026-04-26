import type { ProblemUserStatus } from '@/types/problem';

interface Props {
  status?: ProblemUserStatus | null;
}

export default function ProblemUserStatusBadge({ status }: Props) {
  if (!status) return null;
  if (status === 'SOLVED') {
    return (
      <span
        title="해결한 문제"
        className="inline-flex items-center rounded-full border border-green-500/30 bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700"
      >
        해결
      </span>
    );
  }
  return (
    <span
      title="제출 이력은 있으나 아직 해결하지 못한 문제"
      className="inline-flex items-center rounded-full border border-red-500/30 bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700"
    >
      틀림
    </span>
  );
}
