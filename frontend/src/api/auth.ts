import { apiClient } from './client';
import { useAuthStore } from '@/stores/authStore';
import type { LoginRequest, MeResponse, SignUpRequest, TokenResponse } from '@/types/auth';
import type { ApiResponse } from '@/types/common';

export async function signUp(req: SignUpRequest) {
  const { data } = await apiClient.post<ApiResponse<{ id: number; email: string; username: string }>>(
    '/auth/signup',
    req
  );
  return data.data;
}

export async function login(req: LoginRequest): Promise<TokenResponse> {
  const { data } = await apiClient.post<ApiResponse<TokenResponse>>('/auth/login', req);
  return data.data;
}

export async function fetchMe(): Promise<MeResponse> {
  const { data } = await apiClient.get<ApiResponse<MeResponse>>('/users/me');
  return data.data;
}

export async function logout() {
  const { refreshToken, logout: localLogout } = useAuthStore.getState();
  try {
    await apiClient.post('/auth/logout', refreshToken ? { refreshToken } : undefined);
  } catch {
    // 서버 측 실패해도 로컬은 비움
  }
  localLogout();
}
