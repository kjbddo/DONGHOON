import { Navigate, Outlet } from 'react-router-dom';

import { isAdmin, useAuthStore } from '@/stores/authStore';

export default function AdminGuard() {
  const { user } = useAuthStore();
  if (!isAdmin(user)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
