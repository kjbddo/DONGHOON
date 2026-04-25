import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { AxiosError } from 'axios';

import { fetchMe, login } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';

interface FormValues {
  email: string;
  password: string;
}

export default function LoginPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setTokens, setUser } = useAuthStore();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>();

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      const tokens = await login(values);
      setTokens(tokens.accessToken, tokens.refreshToken);
      const me = await fetchMe();
      setUser({ userId: me.id, email: me.email, username: me.username, roles: me.roles });
      const redirect = params.get('redirect') || '/';
      navigate(redirect, { replace: true });
    } catch (e) {
      const ax = e as AxiosError<{ error?: { message?: string } }>;
      setServerError(ax.response?.data?.error?.message ?? '로그인에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-sm mx-auto py-12">
      <h1 className="text-2xl font-bold mb-6">로그인</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">이메일</label>
          <input
            type="email"
            autoComplete="email"
            className="w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            {...register('email', {
              required: '이메일을 입력하세요.',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '이메일 형식이 올바르지 않습니다.' },
            })}
          />
          {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">비밀번호</label>
          <input
            type="password"
            autoComplete="current-password"
            className="w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            {...register('password', { required: '비밀번호를 입력하세요.' })}
          />
          {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
        </div>
        {serverError && <p className="text-sm text-red-600">{serverError}</p>}
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-blue-600 text-white rounded-md py-2 font-medium hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? '로그인 중…' : '로그인'}
        </button>
      </form>
      <p className="mt-4 text-sm text-gray-500 text-center">
        계정이 없으신가요?{' '}
        <Link to="/signup" className="text-blue-600 hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}
