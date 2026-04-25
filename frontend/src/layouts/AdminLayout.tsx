import { Link, NavLink, Outlet } from 'react-router-dom';

import { useAuthStore } from '@/stores/authStore';

const NAV = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/problems', label: '문제 관리' },
  { to: '/admin/problems/ai-generate', label: 'AI 문제 생성' },
  { to: '/admin/problem-imports', label: '문제 불러오기' },
];

export default function AdminLayout() {
  const { user, logout } = useAuthStore();
  return (
    <div className="min-h-screen flex">
      <aside className="w-60 bg-gray-900 text-gray-100 p-4 flex flex-col">
        <Link to="/admin" className="font-bold text-lg mb-1">
          AlgoForge Admin
        </Link>
        {user && (
          <div className="text-xs text-gray-400 mb-6">
            {user.username} <span className="text-purple-300">({user.email})</span>
          </div>
        )}
        <nav className="flex flex-col gap-1 text-sm">
          {NAV.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) =>
                `px-3 py-2 rounded transition ${
                  isActive
                    ? 'bg-gray-800 text-white'
                    : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                }`
              }
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto pt-4 border-t border-gray-800 flex flex-col gap-2 text-xs">
          <Link to="/" className="text-gray-400 hover:text-white">
            ← 사용자 사이트로
          </Link>
          <button
            type="button"
            onClick={logout}
            className="text-gray-400 hover:text-red-400 text-left"
          >
            로그아웃
          </button>
        </div>
      </aside>
      <main className="flex-1 p-6 bg-gray-50 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
