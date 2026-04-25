import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  changeProblemStatus,
  deleteAdminProblem,
  fetchAdminProblems,
} from '@/api/admin';
import {
  DIFFICULTY_OPTIONS,
  PROBLEM_STATUS_OPTIONS,
  STATUS_COLOR,
  STATUS_LABEL,
} from '@/types/admin';
import type { Difficulty, ProblemStatus } from '@/types/problem';

interface Filters {
  page: number;
  size: number;
  status?: ProblemStatus;
  difficulty?: Difficulty;
  ai?: boolean;
  includeDeleted: boolean;
  keyword: string;
}

const DEFAULT_FILTERS: Filters = {
  page: 0,
  size: 20,
  includeDeleted: false,
  keyword: '',
};

export default function AdminProblemManagePage() {
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);
  const [keywordInput, setKeywordInput] = useState('');
  const qc = useQueryClient();

  const query = useQuery({
    queryKey: ['admin', 'problems', filters],
    queryFn: () => fetchAdminProblems(filters),
  });

  const statusMutation = useMutation({
    mutationFn: (vars: { id: number; status: ProblemStatus }) =>
      changeProblemStatus(vars.id, vars.status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'problems'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteAdminProblem(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'problems'] }),
  });

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((prev) => ({ ...prev, page: 0, keyword: keywordInput.trim() }));
  };

  const onResetFilters = () => {
    setFilters(DEFAULT_FILTERS);
    setKeywordInput('');
  };

  const data = query.data;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">문제 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            전체 문제 목록 / 상태 변경 / 소프트 삭제 / AI 생성된 문제 검토
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/admin/problems/new"
            className="px-4 py-2 rounded bg-blue-600 text-white text-sm font-medium hover:bg-blue-700"
          >
            + 새 문제
          </Link>
          <Link
            to="/admin/problems/ai-generate"
            className="px-4 py-2 rounded bg-purple-600 text-white text-sm font-medium hover:bg-purple-700"
          >
            ✨ AI로 생성
          </Link>
        </div>
      </div>

      <form
        onSubmit={onSearch}
        className="grid grid-cols-1 md:grid-cols-6 gap-3 bg-white border rounded p-4 items-end"
      >
        <div className="md:col-span-2">
          <label className="block text-xs text-gray-500 mb-1">키워드</label>
          <input
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="제목/슬러그 검색"
            className="w-full border rounded px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">상태</label>
          <select
            value={filters.status ?? ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                page: 0,
                status: (e.target.value || undefined) as ProblemStatus | undefined,
              }))
            }
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="">전체</option>
            {PROBLEM_STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">난이도</label>
          <select
            value={filters.difficulty ?? ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                page: 0,
                difficulty: (e.target.value || undefined) as Difficulty | undefined,
              }))
            }
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="">전체</option>
            {DIFFICULTY_OPTIONS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">생성 유형</label>
          <select
            value={filters.ai === undefined ? '' : filters.ai ? 'ai' : 'human'}
            onChange={(e) => {
              const v = e.target.value;
              setFilters((prev) => ({
                ...prev,
                page: 0,
                ai: v === '' ? undefined : v === 'ai',
              }));
            }}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="">전체</option>
            <option value="ai">AI 생성</option>
            <option value="human">관리자 작성</option>
          </select>
        </div>
        <div className="flex items-end gap-2">
          <button
            type="submit"
            className="px-4 py-2 bg-gray-900 text-white text-sm rounded hover:bg-black"
          >
            검색
          </button>
          <button
            type="button"
            onClick={onResetFilters}
            className="px-4 py-2 bg-white border text-sm rounded hover:bg-gray-50"
          >
            초기화
          </button>
        </div>
        <label className="md:col-span-6 inline-flex items-center gap-2 text-sm text-gray-600 mt-1">
          <input
            type="checkbox"
            checked={filters.includeDeleted}
            onChange={(e) =>
              setFilters((prev) => ({ ...prev, page: 0, includeDeleted: e.target.checked }))
            }
          />
          삭제된 문제 포함
        </label>
      </form>

      <div className="bg-white border rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b text-gray-600">
            <tr>
              <th className="px-3 py-2 text-left w-16">ID</th>
              <th className="px-3 py-2 text-left">제목 / Slug</th>
              <th className="px-3 py-2 text-left w-24">난이도</th>
              <th className="px-3 py-2 text-left w-32">상태</th>
              <th className="px-3 py-2 text-left w-24">유형</th>
              <th className="px-3 py-2 text-right w-16">신고</th>
              <th className="px-3 py-2 text-right w-64">액션</th>
            </tr>
          </thead>
          <tbody>
            {query.isLoading && (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            )}
            {query.isError && (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-red-500">
                  목록을 불러오지 못했습니다.
                </td>
              </tr>
            )}
            {data && data.empty && (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-gray-400">
                  조건에 맞는 문제가 없습니다.
                </td>
              </tr>
            )}
            {data?.content.map((p) => (
              <tr key={p.id} className="border-b last:border-b-0 hover:bg-gray-50">
                <td className="px-3 py-2 text-gray-500">{p.id}</td>
                <td className="px-3 py-2">
                  <div className="font-medium text-gray-900">{p.title}</div>
                  <div className="text-xs text-gray-400">{p.slug}</div>
                </td>
                <td className="px-3 py-2">{p.difficulty}</td>
                <td className="px-3 py-2">
                  <span
                    className={`inline-block text-xs border rounded px-2 py-0.5 ${STATUS_COLOR[p.status]}`}
                  >
                    {STATUS_LABEL[p.status]}
                  </span>
                </td>
                <td className="px-3 py-2 text-xs">
                  {p.aiGenerated ? (
                    <span className="text-purple-700 font-semibold">AI</span>
                  ) : (
                    <span className="text-gray-500">관리자</span>
                  )}
                </td>
                <td className="px-3 py-2 text-right">{p.reportCount}</td>
                <td className="px-3 py-2 text-right">
                  <div className="inline-flex gap-1 items-center">
                    <select
                      value={p.status}
                      disabled={statusMutation.isPending}
                      onChange={(e) =>
                        statusMutation.mutate({ id: p.id, status: e.target.value as ProblemStatus })
                      }
                      className="border rounded px-2 py-1 text-xs"
                      title="상태 변경"
                    >
                      {PROBLEM_STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>
                          {STATUS_LABEL[s]}
                        </option>
                      ))}
                    </select>
                    <Link
                      to={`/admin/problems/${p.id}`}
                      className="px-2 py-1 text-xs rounded border hover:bg-gray-100"
                    >
                      편집
                    </Link>
                    <button
                      type="button"
                      disabled={deleteMutation.isPending}
                      onClick={() => {
                        if (window.confirm(`정말 "${p.title}" 문제를 삭제하시겠습니까?`)) {
                          deleteMutation.mutate(p.id);
                        }
                      }}
                      className="px-2 py-1 text-xs rounded border border-red-300 text-red-600 hover:bg-red-50"
                    >
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            type="button"
            disabled={data.first}
            onClick={() => setFilters((prev) => ({ ...prev, page: Math.max(0, prev.page - 1) }))}
            className="px-3 py-1 text-sm border rounded disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-sm text-gray-600">
            {data.number + 1} / {data.totalPages}
          </span>
          <button
            type="button"
            disabled={data.last}
            onClick={() => setFilters((prev) => ({ ...prev, page: prev.page + 1 }))}
            className="px-3 py-1 text-sm border rounded disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
