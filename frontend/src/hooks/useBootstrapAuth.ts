import { useEffect } from 'react';

import { fetchMe } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';

/**
 * 새로고침 시 토큰만 있고 user 가 없으면 /users/me 를 호출해 채워 둔다.
 * 401 발생 시 client 인터셉터가 refresh 후 재시도하고, 그래도 실패하면 logout.
 */
export function useBootstrapAuth() {
  const { accessToken, user, setUser, logout } = useAuthStore();

  useEffect(() => {
    if (!accessToken || user) return;
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        setUser({
          userId: me.id,
          email: me.email,
          username: me.username,
          roles: me.roles,
        });
      })
      .catch(() => {
        if (!cancelled) logout();
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, user, setUser, logout]);
}
