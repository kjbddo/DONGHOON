import ReactMarkdown, { type Components } from 'react-markdown';
import remarkBreaks from 'remark-breaks';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';

interface Props {
  /** 문제 본문 / 입출력 / 제약 등에 사용되는 마크다운 텍스트. */
  children?: string | null;
  /** 별도 클래스로 prose 크기 등을 바꾸고 싶을 때 사용. 기본은 prose-sm 와 동일. */
  className?: string;
  /**
   * `<li>` 등 인라인 자리에서 사용할 때 true.
   * `<p>` 의 기본 마진을 제거하고 prose wrapper 도 제거한다.
   */
  inline?: boolean;
}

/**
 * 문제 콘텐츠 전용 Markdown 렌더러.
 *
 * - GFM (테이블, 체크박스, 자동 링크 등) 지원
 * - KaTeX 수식 지원: 인라인 `$...$`, 블록 `$$...$$`
 * - 외부 이미지를 마크다운 문법(`![설명](https://...)`) 으로 표기 가능
 *   - 안정성을 위해 외부 도메인만 허용하고, 누락된 alt 는 빈 문자열로 채움.
 * - AI/관리자 본문은 단일 줄바꿈을 유지하는 편이 자연스러우므로 `remark-breaks`
 *   를 추가해 single newline → `<br>` 로 변환한다.
 *
 * 콘텐츠는 관리자 또는 AI 가 작성하므로 사용자 입력 XSS 위험은 낮지만,
 * `react-markdown` 기본 정책상 raw HTML 은 무시한다.
 */
export default function ProblemMarkdown({ children, className, inline = false }: Props) {
  const text = children ?? '';
  const wrapperClass = inline
    ? `katex-host break-words ${className ?? ''}`.trim()
    : `prose prose-sm max-w-none break-words katex-host ${className ?? ''}`.trim();

  return (
    <div className={wrapperClass}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkBreaks, remarkMath]}
        rehypePlugins={[[rehypeKatex, REHYPE_KATEX_OPTIONS]]}
        components={inline ? INLINE_COMPONENTS : BLOCK_COMPONENTS}
      >
        {text}
      </ReactMarkdown>
    </div>
  );
}

/**
 * KaTeX 가 알 수 없는 매크로/문자를 만나도 빨간 에러 박스 대신 원문을 그대로
 * 표시하도록 한다. (`throwOnError: false` + `strict: 'ignore'`)
 */
const REHYPE_KATEX_OPTIONS = {
  throwOnError: false,
  strict: 'ignore' as const,
  output: 'html' as const,
};

const IMG_RENDERER: Components['img'] = ({ src, alt, title }) => {
  if (!src) return null;
  if (!isSafeImageUrl(String(src))) {
    return <span className="text-xs text-gray-400">[이미지 생략: {alt || String(src)}]</span>;
  }
  return (
    <img
      src={String(src)}
      alt={alt ?? ''}
      title={title}
      loading="lazy"
      className="my-2 max-w-full rounded border"
    />
  );
};

const ANCHOR_RENDERER: Components['a'] = ({ href, children }) => (
  <a href={href} target="_blank" rel="noreferrer noopener">
    {children}
  </a>
);

const BLOCK_COMPONENTS: Components = {
  img: IMG_RENDERER,
  a: ANCHOR_RENDERER,
};

/**
 * 인라인 모드에서는 `<p>` 가 들어가면 부모 `<li>` 의 줄높이가 깨지므로
 * `<p>` 를 그냥 자식으로 펼쳐서 렌더한다.
 */
const INLINE_COMPONENTS: Components = {
  img: IMG_RENDERER,
  a: ANCHOR_RENDERER,
  p: ({ children }) => <>{children}</>,
};

function isSafeImageUrl(src: string): boolean {
  const trimmed = src.trim();
  return (
    trimmed.startsWith('https://') ||
    trimmed.startsWith('http://') ||
    trimmed.startsWith('/')
  );
}
