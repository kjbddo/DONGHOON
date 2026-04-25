import type { Difficulty } from './problem';
import type { SubmissionStatus } from './submission';

export interface UserStats {
  userId: number;
  username: string;
  solvedCount: number;
  attemptedCount: number;
  totalSubmissions: number;
  acceptanceRate: number;
  bookmarkCount: number;
  rank: number;
  solvedByDifficulty: Partial<Record<Difficulty, number>>;
  submissionsByStatus: Partial<Record<SubmissionStatus, number>>;
  languageUsage: Record<string, number>;
}

export interface RankingEntry {
  rank: number;
  userId: number;
  username: string;
  profileImageUrl?: string;
  solvedCount: number;
}
