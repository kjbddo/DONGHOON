import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';

import { signUp } from '@/api/auth';

interface FormValues {
  email: string;
  username: string;
  password: string;
  passwordConfirm: string;
}

export default function SignUpPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>();

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      await signUp({ email: values.email, username: values.username, password: values.password });
      navigate('/login', { replace: true });
    } catch (e) {
      const ax = e as AxiosError<{ error?: { message?: string } }>;
      setServerError(ax.response?.data?.error?.message ?? '회원가입에 실패했습니다.');
    }
  };

  const passwordValue = watch('password');

  return (
    <div className="max-w-sm mx-auto py-12">
      <h1 className="text-2xl font-bold mb-6">회원가입</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Field
          label="이메일"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email', {
            required: '이메일을 입력하세요.',
            pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '이메일 형식이 올바르지 않습니다.' },
          })}
        />
        <Field
          label="사용자명"
          autoComplete="username"
          error={errors.username?.message}
          {...register('username', {
            required: '사용자명을 입력하세요.',
            minLength: { value: 2, message: '2자 이상 입력하세요.' },
            maxLength: { value: 50, message: '50자 이하로 입력하세요.' },
          })}
        />
        <Field
          label="비밀번호"
          type="password"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password', {
            required: '비밀번호를 입력하세요.',
            minLength: { value: 8, message: '8자 이상 입력하세요.' },
          })}
        />
        <Field
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          error={errors.passwordConfirm?.message}
          {...register('passwordConfirm', {
            required: '비밀번호를 한 번 더 입력하세요.',
            validate: (v) => v === passwordValue || '비밀번호가 일치하지 않습니다.',
          })}
        />
        {serverError && <p className="text-sm text-red-600">{serverError}</p>}
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-blue-600 text-white rounded-md py-2 font-medium hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? '가입 중…' : '회원가입'}
        </button>
      </form>
      <p className="mt-4 text-sm text-gray-500 text-center">
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="text-blue-600 hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}

import { forwardRef, InputHTMLAttributes } from 'react';

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

const Field = forwardRef<HTMLInputElement, FieldProps>(({ label, error, ...rest }, ref) => (
  <div>
    <label className="block text-sm font-medium mb-1">{label}</label>
    <input
      ref={ref}
      className="w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
      {...rest}
    />
    {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
  </div>
));
Field.displayName = 'Field';
