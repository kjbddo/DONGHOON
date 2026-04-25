import { Link, NavLink, Outlet } from 'react-router-dom';

import { logout as serverLogout } from '@/api/auth';
import { useBootstrapAuth } from '@/hooks/useBootstrapAuth';
import { useAuthStore } from '@/stores/authStore';

export default function UserLayout() {
  useBootstrapAuth();
  const { user } = useAuthStore();

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link to="/" className="font-bold text-lg">
            AlgoForge
          </Link>
          <nav className="flex gap-5 items-center text-sm">
            <NavLink
              to="/problems"
              className={({ isActive }) => (isActive ? 'text-blue-600 font-medium' : 'text-gray-700 hover:text-gray-900')}
            >
              문제
            </NavLink>
            <NavLink
              to="/ranking"
              className={({ isActive }) => (isActive ? 'text-blue-600 font-medium' : 'text-gray-700 hover:text-gray-900')}
            >
              랭킹
            </NavLink>
            {user ? (
              <>
                <NavLink
                  to="/submissions"
                  className={({ isActive }) =>
                    isActive ? 'text-blue-600 font-medium' : 'text-gray-700 hover:text-gray-900'
                  }
                >
                  제출
                </NavLink>
                <NavLink
                  to="/bookmarks"
                  className={({ isActive }) =>
                    isActive ? 'text-blue-600 font-medium' : 'text-gray-700 hover:text-gray-900'
                  }
                >
                  북마크
                </NavLink>
                <NavLink
                  to="/mypage"
                  className={({ isActive }) =>
                    isActive ? 'text-blue-600 font-medium' : 'text-gray-700 hover:text-gray-900'
                  }
                >
                  마이페이지
                </NavLink>
                {user.roles.includes('ROLE_ADMIN') && (
                  <Link to="/admin" className="text-purple-600 font-medium">
                    관리자
                  </Link>
                )}
                <span className="text-gray-400">|</span>
                <span className="text-gray-500 text-xs">{user.username}</span>
                <button
                  onClick={() => serverLogout()}
                  className="text-gray-500 hover:text-gray-900"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-gray-700 hover:text-gray-900">
                  로그인
                </Link>
                <Link
                  to="/signup"
                  className="px-3 py-1 rounded-md bg-blue-600 text-white text-sm hover:bg-blue-700"
                >
                  회원가입
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1 max-w-6xl mx-auto w-full px-4 py-6">
        <Outlet />
      </main>
      <footer className="border-t py-4 text-center text-xs text-gray-400">
        © {new Date().getFullYear()} AlgoForge
      </footer>
    </div>
  );
}
