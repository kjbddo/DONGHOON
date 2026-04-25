import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createAdminProblem,
  fetchAdminProblem,
  updateAdminProblem,
} from '@/api/admin';
import {
  DIFFICULTY_OPTIONS,
  SOURCE_TYPE_OPTIONS,
  type AdminProblemDetail,
  type AdminProblemUpsertPayload,
  type AdminTestCase,
} from '@/types/admin';
import type { Difficulty, Example, SourceType } from '@/types/problem';

interface FormState {
  title: string;
  slug: string;
  description: string;
  inputDescription: string;
  outputDescription: string;
  constraintsText: string;       // 라인 단위
  examples: Example[];
  timeLimitMs: number;
  memoryLimitMb: number;
  difficulty: Difficulty;
  sourceType: SourceType;
  categoriesText: string;        // 콤마 구분
  tagsText: string;              // 콤마 구분
  testCases: AdminTestCase[];
}

const EMPTY_FORM: FormState = {
  title: '',
  slug: '',
  description: '',
  inputDescription: '',
  outputDescription: '',
  constraintsText: '',
  examples: [{ input: '', output: '', explanation: '' }],
  timeLimitMs: 2000,
  memoryLimitMb: 256,
  difficulty: 'BRONZE',
  sourceType: 'ADMIN_CREATED',
  categoriesText: '',
  tagsText: '',
  testCases: [{ seq: 1, input: '', expectedOutput: '', hidden: false }],
};

function detailToForm(d: AdminProblemDetail): FormState {
  return {
    title: d.title,
    slug: d.slug,
    description: d.description,
    inputDescription: d.inputDescription,
    outputDescription: d.outputDescription,
    constraintsText: d.constraints.join('\n'),
    examples: d.examples.length ? d.examples : [{ input: '', output: '', explanation: '' }],
    timeLimitMs: d.timeLimitMs,
    memoryLimitMb: d.memoryLimitMb,
    difficulty: d.difficulty,
    sourceType: d.sourceType,
    categoriesText: d.categories.join(', '),
    tagsText: d.tags.join(', '),
    testCases: d.testCases.length
      ? d.testCases.map((t) => ({ ...t }))
      : [{ seq: 1, input: '', expectedOutput: '', hidden: false }],
  };
}

function formToPayload(f: FormState, isCreate: boolean): AdminProblemUpsertPayload {
  const categories = f.categoriesText
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean);
  const tags = f.tagsText
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean);
  const constraints = f.constraintsText
    .split('\n')
    .map((c) => c.trim())
    .filter(Boolean);

  const base: AdminProblemUpsertPayload = {
    title: f.title.trim(),
    description: f.description,
    inputDescription: f.inputDescription,
    outputDescription: f.outputDescription,
    constraints,
    examples: f.examples
      .map((e) => ({
        input: e.input,
        output: e.output,
        explanation: e.explanation || undefined,
      }))
      .filter((e) => e.input || e.output),
    timeLimitMs: Number(f.timeLimitMs) || 2000,
    memoryLimitMb: Number(f.memoryLimitMb) || 256,
    difficulty: f.difficulty,
    categories,
    tags,
    testCases: f.testCases.map((t, i) => ({
      seq: t.seq || i + 1,
      input: t.input,
      expectedOutput: t.expectedOutput,
      hidden: t.hidden,
    })),
  };

  if (isCreate) {
    base.sourceType = f.sourceType;
    if (f.slug.trim()) base.slug = f.slug.trim();
  }
  return base;
}

export default function AdminProblemFormPage() {
  const params = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const isCreate = !params.id;
  const idNum = params.id ? Number(params.id) : null;

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const detailQuery = useQuery({
    enabled: !isCreate && idNum != null,
    queryKey: ['admin', 'problem', idNum],
    queryFn: () => fetchAdminProblem(idNum as number),
  });

  useEffect(() => {
    if (detailQuery.data) setForm(detailToForm(detailQuery.data));
  }, [detailQuery.data]);

  const mutation = useMutation({
    mutationFn: (payload: AdminProblemUpsertPayload) =>
      isCreate ? createAdminProblem(payload) : updateAdminProblem(idNum as number, payload),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ['admin', 'problems'] });
      qc.invalidateQueries({ queryKey: ['admin', 'problem', saved.id] });
      navigate('/admin/problems');
    },
    onError: (err: unknown) => {
      const message = (err as { response?: { data?: { error?: { message?: string } } } })
        ?.response?.data?.error?.message;
      setErrorMsg(message ?? '저장에 실패했습니다.');
    },
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!form.title.trim()) {
      setErrorMsg('제목은 필수입니다.');
      return;
    }
    if (form.testCases.length === 0) {
      setErrorMsg('테스트 케이스가 1개 이상 필요합니다.');
      return;
    }
    mutation.mutate(formToPayload(form, isCreate));
  };

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  /* ---- example handlers ---- */
  const addExample = () =>
    setField('examples', [...form.examples, { input: '', output: '', explanation: '' }]);
  const removeExample = (idx: number) =>
    setField(
      'examples',
      form.examples.filter((_, i) => i !== idx)
    );
  const updateExample = (idx: number, patch: Partial<Example>) =>
    setField(
      'examples',
      form.examples.map((e, i) => (i === idx ? { ...e, ...patch } : e))
    );

  /* ---- testcase handlers ---- */
  const addTestCase = () =>
    setField('testCases', [
      ...form.testCases,
      { seq: form.testCases.length + 1, input: '', expectedOutput: '', hidden: false },
    ]);
  const removeTestCase = (idx: number) =>
    setField(
      'testCases',
      form.testCases.filter((_, i) => i !== idx).map((t, i) => ({ ...t, seq: i + 1 }))
    );
  const updateTestCase = (idx: number, patch: Partial<AdminTestCase>) =>
    setField(
      'testCases',
      form.testCases.map((t, i) => (i === idx ? { ...t, ...patch } : t))
    );

  const title = useMemo(
    () => (isCreate ? '새 문제 작성' : `문제 편집 #${idNum}`),
    [isCreate, idNum]
  );

  if (!isCreate && detailQuery.isLoading) {
    return <div className="text-gray-500">불러오는 중…</div>;
  }
  if (!isCreate && detailQuery.isError) {
    return <div className="text-red-500">문제를 불러오지 못했습니다.</div>;
  }

  return (
    <form onSubmit={onSubmit} className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{title}</h1>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => navigate('/admin/problems')}
            className="px-4 py-2 text-sm border rounded hover:bg-gray-50"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {mutation.isPending ? '저장 중…' : isCreate ? '문제 생성' : '변경 저장'}
          </button>
        </div>
      </div>

      {errorMsg && (
        <div className="border border-red-300 bg-red-50 text-red-700 text-sm rounded p-3">
          {errorMsg}
        </div>
      )}

      {/* 기본 정보 */}
      <section className="bg-white border rounded p-4 space-y-4">
        <h2 className="font-semibold text-gray-700">기본 정보</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Field label="제목 *">
            <input
              value={form.title}
              onChange={(e) => setField('title', e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm"
              placeholder="예: 두 수의 합"
            />
          </Field>
          <Field label={isCreate ? '슬러그 (비워두면 자동 생성)' : '슬러그 (수정 불가)'}>
            <input
              value={form.slug}
              disabled={!isCreate}
              onChange={(e) => setField('slug', e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm disabled:bg-gray-100"
              placeholder="two-sum-basic"
            />
          </Field>
          <Field label="난이도 *">
            <select
              value={form.difficulty}
              onChange={(e) => setField('difficulty', e.target.value as Difficulty)}
              className="w-full border rounded px-3 py-2 text-sm"
            >
              {DIFFICULTY_OPTIONS.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </Field>
          {isCreate && (
            <Field label="소스 유형">
              <select
                value={form.sourceType}
                onChange={(e) => setField('sourceType', e.target.value as SourceType)}
                className="w-full border rounded px-3 py-2 text-sm"
              >
                {SOURCE_TYPE_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </Field>
          )}
          <Field label="시간 제한(ms)">
            <input
              type="number"
              min={100}
              value={form.timeLimitMs}
              onChange={(e) => setField('timeLimitMs', Number(e.target.value))}
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </Field>
          <Field label="메모리 제한(MB)">
            <input
              type="number"
              min={16}
              value={form.memoryLimitMb}
              onChange={(e) => setField('memoryLimitMb', Number(e.target.value))}
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </Field>
          <Field label="카테고리 (콤마 구분)">
            <input
              value={form.categoriesText}
              onChange={(e) => setField('categoriesText', e.target.value)}
              placeholder="구현, 수학"
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </Field>
          <Field label="태그 (콤마 구분)">
            <input
              value={form.tagsText}
              onChange={(e) => setField('tagsText', e.target.value)}
              placeholder="implementation, math"
              className="w-full border rounded px-3 py-2 text-sm"
            />
          </Field>
        </div>
      </section>

      {/* 본문 */}
      <section className="bg-white border rounded p-4 space-y-4">
        <h2 className="font-semibold text-gray-700">문제 설명 (Markdown)</h2>
        <Field label="본문 *">
          <textarea
            value={form.description}
            onChange={(e) => setField('description', e.target.value)}
            rows={10}
            className="w-full border rounded px-3 py-2 text-sm font-mono"
          />
        </Field>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Field label="입력 형식 *">
            <textarea
              value={form.inputDescription}
              onChange={(e) => setField('inputDescription', e.target.value)}
              rows={5}
              className="w-full border rounded px-3 py-2 text-sm font-mono"
            />
          </Field>
          <Field label="출력 형식 *">
            <textarea
              value={form.outputDescription}
              onChange={(e) => setField('outputDescription', e.target.value)}
              rows={5}
              className="w-full border rounded px-3 py-2 text-sm font-mono"
            />
          </Field>
        </div>
        <Field label="제약 조건 (한 줄에 하나씩)">
          <textarea
            value={form.constraintsText}
            onChange={(e) => setField('constraintsText', e.target.value)}
            rows={4}
            placeholder={'1 ≤ N ≤ 100\n-10^9 ≤ A, B ≤ 10^9'}
            className="w-full border rounded px-3 py-2 text-sm font-mono"
          />
        </Field>
      </section>

      {/* 예제 */}
      <section className="bg-white border rounded p-4 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-gray-700">예제 입출력 (사용자에게 공개)</h2>
          <button
            type="button"
            onClick={addExample}
            className="px-3 py-1 text-xs border rounded hover:bg-gray-100"
          >
            + 예제 추가
          </button>
        </div>
        {form.examples.map((ex, i) => (
          <div key={i} className="grid grid-cols-1 md:grid-cols-3 gap-2 border rounded p-3">
            <Field label={`입력 #${i + 1}`}>
              <textarea
                value={ex.input}
                onChange={(e) => updateExample(i, { input: e.target.value })}
                rows={3}
                className="w-full border rounded px-2 py-1 text-xs font-mono"
              />
            </Field>
            <Field label="출력">
              <textarea
                value={ex.output}
                onChange={(e) => updateExample(i, { output: e.target.value })}
                rows={3}
                className="w-full border rounded px-2 py-1 text-xs font-mono"
              />
            </Field>
            <Field label="설명 (옵션)">
              <textarea
                value={ex.explanation ?? ''}
                onChange={(e) => updateExample(i, { explanation: e.target.value })}
                rows={3}
                className="w-full border rounded px-2 py-1 text-xs"
              />
            </Field>
            <div className="md:col-span-3 flex justify-end">
              <button
                type="button"
                onClick={() => removeExample(i)}
                className="text-xs text-red-600 hover:underline"
              >
                예제 삭제
              </button>
            </div>
          </div>
        ))}
      </section>

      {/* 테스트 케이스 */}
      <section className="bg-white border rounded p-4 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-gray-700">
            테스트 케이스 (채점에 사용 — hidden 체크 시 사용자에게 비공개)
          </h2>
          <button
            type="button"
            onClick={addTestCase}
            className="px-3 py-1 text-xs border rounded hover:bg-gray-100"
          >
            + 케이스 추가
          </button>
        </div>
        {form.testCases.map((tc, i) => (
          <div key={i} className="grid grid-cols-1 md:grid-cols-2 gap-2 border rounded p-3">
            <Field label={`입력 #${tc.seq}`}>
              <textarea
                value={tc.input}
                onChange={(e) => updateTestCase(i, { input: e.target.value })}
                rows={4}
                className="w-full border rounded px-2 py-1 text-xs font-mono"
              />
            </Field>
            <Field label="기대 출력">
              <textarea
                value={tc.expectedOutput}
                onChange={(e) => updateTestCase(i, { expectedOutput: e.target.value })}
                rows={4}
                className="w-full border rounded px-2 py-1 text-xs font-mono"
              />
            </Field>
            <div className="md:col-span-2 flex items-center justify-between text-xs">
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={tc.hidden}
                  onChange={(e) => updateTestCase(i, { hidden: e.target.checked })}
                />
                hidden (사용자에게 비공개)
              </label>
              <button
                type="button"
                onClick={() => removeTestCase(i)}
                className="text-red-600 hover:underline"
              >
                케이스 삭제
              </button>
            </div>
          </div>
        ))}
      </section>
    </form>
  );
}

interface FieldProps {
  label: string;
  children: React.ReactNode;
}
function Field({ label, children }: FieldProps) {
  return (
    <label className="block">
      <span className="block text-xs text-gray-500 mb-1">{label}</span>
      {children}
    </label>
  );
}
