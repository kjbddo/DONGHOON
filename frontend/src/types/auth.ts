export interface SignUpRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface MeResponse {
  id: number;
  email: string;
  username: string;
  profileImageUrl?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  roles: string[];
}
