import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface AuthUser {
  userId: number;
  email: string;
  username: string;
  roles: string[];
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      setUser: (user) => set({ user }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'algoforge-auth' }
  )
);

export const isAdmin = (user: AuthUser | null) => !!user?.roles?.includes('ROLE_ADMIN');
