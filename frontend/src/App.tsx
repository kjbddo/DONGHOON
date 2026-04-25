import { Route, Routes } from 'react-router-dom';

import UserLayout from '@/layouts/UserLayout';
import AdminLayout from '@/layouts/AdminLayout';
import AuthGuard from '@/routes/guards/AuthGuard';
import AdminGuard from '@/routes/guards/AdminGuard';

import HomePage from '@/pages/home/HomePage';
import LoginPage from '@/pages/auth/LoginPage';
import SignUpPage from '@/pages/auth/SignUpPage';
import ProblemListPage from '@/pages/problems/ProblemListPage';
import ProblemDetailPage from '@/pages/problems/ProblemDetailPage';
import ProblemSolvePage from '@/pages/problems/ProblemSolvePage';
import SubmissionListPage from '@/pages/submissions/SubmissionListPage';
import SubmissionDetailPage from '@/pages/submissions/SubmissionDetailPage';
import MyPage from '@/pages/mypage/MyPage';
import SolvedProblemsPage from '@/pages/mypage/SolvedProblemsPage';
import RankingPage from '@/pages/ranking/RankingPage';
import BookmarksPage from '@/pages/bookmarks/BookmarksPage';
import UserProfilePage from '@/pages/users/UserProfilePage';

import AdminDashboardPage from '@/pages/admin/AdminDashboardPage';
import AdminProblemManagePage from '@/pages/admin/AdminProblemManagePage';
import AdminProblemFormPage from '@/pages/admin/AdminProblemFormPage';
import AdminAiProblemGeneratePage from '@/pages/admin/AdminAiProblemGeneratePage';
import AdminProblemImportPage from '@/pages/admin/AdminProblemImportPage';

export default function App() {
  return (
    <Routes>
      <Route element={<UserLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/problems" element={<ProblemListPage />} />
        <Route path="/problems/:id" element={<ProblemDetailPage />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/users/:userId" element={<UserProfilePage />} />

        <Route element={<AuthGuard />}>
          <Route path="/problems/:id/solve" element={<ProblemSolvePage />} />
          <Route path="/submissions" element={<SubmissionListPage />} />
          <Route path="/submissions/:id" element={<SubmissionDetailPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/mypage/solved" element={<SolvedProblemsPage />} />
          <Route path="/bookmarks" element={<BookmarksPage />} />
        </Route>
      </Route>

      <Route element={<AdminGuard />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/problems" element={<AdminProblemManagePage />} />
          <Route path="/admin/problems/new" element={<AdminProblemFormPage />} />
          <Route path="/admin/problems/ai-generate" element={<AdminAiProblemGeneratePage />} />
          <Route path="/admin/problems/:id" element={<AdminProblemFormPage />} />
          <Route path="/admin/problem-imports" element={<AdminProblemImportPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
