import axios, { AxiosError, AxiosRequestConfig } from 'axios';

import { useAuthStore } from '@/stores/authStore';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15_000,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setTokens, logout } = useAuthStore.getState();
  if (!refreshToken) {
    logout();
    return null;
  }
  try {
    const { data } = await axios.post('/api/auth/refresh', { refreshToken });
    const newAccess = data?.data?.accessToken as string;
    const newRefresh = data?.data?.refreshToken as string;
    if (newAccess && newRefresh) {
      setTokens(newAccess, newRefresh);
      return newAccess;
    }
  } catch {
    // fall through
  }
  logout();
  return null;
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      refreshing ??= refreshAccessToken();
      const newToken = await refreshing;
      refreshing = null;
      if (newToken) {
        original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${newToken}` };
        return apiClient.request(original);
      }
    }
    return Promise.reject(error);
  }
);
