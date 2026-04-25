import { Link } from 'react-router-dom';

import { useAuthStore } from '@/stores/authStore';

export default function HomePage() {
  const { user } = useAuthStore();
  return (
    <div className="space-y-12 py-8">
      <section className="text-center max-w-3xl mx-auto">
        <span className="inline-block text-xs font-medium px-3 py-1 rounded-full bg-purple-100 text-purple-700 mb-4">
          Algorithm Practice × AI Hints
        </span>
        <h1 className="text-4xl font-bold mb-4">
          알고리즘을 풀고, <span className="text-blue-600">AI에게 배우세요.</span>
        </h1>
        <p className="text-gray-600 text-lg mb-8">
          전통 채점 시스템에 AI 기반 단계별 힌트·반례·복잡도 분석을 결합한 학습 플랫폼입니다. <br />
          정답 코드는 알려주지 않아요. 스스로 풀 수 있도록 안내합니다.
        </p>
        <div className="flex gap-3 justify-center">
          <Link
            to="/problems"
            className="px-6 py-3 rounded-md bg-blue-600 text-white text-base font-medium hover:bg-blue-700"
          >
            문제 둘러보기
          </Link>
          {!user && (
            <Link to="/signup" className="px-6 py-3 rounded-md border text-base font-medium hover:bg-gray-50">
              회원가입
            </Link>
          )}
        </div>
      </section>

      <section className="grid md:grid-cols-3 gap-4">
        <Feature
          title="비동기 채점"
          desc="제출 즉시 결과를 기다릴 필요 없이, 채점 진행 상황을 실시간으로 확인할 수 있어요."
        />
        <Feature
          title="단계별 AI 힌트"
          desc="Lv.1 약한 힌트부터 Lv.4 강한 힌트까지, 원하는 만큼만 도움을 받을 수 있어요."
        />
        <Feature
          title="반례 자동 생성"
          desc="틀린 제출을 분석해 놓치기 쉬운 엣지 케이스 입력을 추천해 드려요."
        />
      </section>
    </div>
  );
}

function Feature({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="border rounded-md bg-white p-5">
      <h3 className="font-semibold mb-2">{title}</h3>
      <p className="text-sm text-gray-600">{desc}</p>
    </div>
  );
}
