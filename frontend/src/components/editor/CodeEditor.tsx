import Editor from '@monaco-editor/react';

interface Props {
  value: string;
  onChange: (v: string) => void;
  language?: string;
  height?: string;
}

const LANG_MAP: Record<string, string> = {
  JAVA: 'java',
  PYTHON: 'python',
  CPP: 'cpp',
  JAVASCRIPT: 'javascript',
};

export default function CodeEditor({ value, onChange, language = 'JAVA', height = '60vh' }: Props) {
  return (
    <Editor
      height={height}
      language={LANG_MAP[language] ?? 'plaintext'}
      value={value}
      onChange={(v) => onChange(v ?? '')}
      theme="vs-dark"
      options={{
        fontSize: 14,
        minimap: { enabled: false },
        tabSize: 4,
        automaticLayout: true,
        scrollBeyondLastLine: false,
      }}
    />
  );
}
