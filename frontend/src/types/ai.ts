export interface AiFeedback {
  id: number;
  submissionId: number;
  feedbackLevel: number;
  summary?: string;
  directionHint?: string;
  counterExampleHint?: string;
  complexityHint?: string;
  runtimeErrorHint?: string;
  compileErrorHint?: string;
  createdAt: string;
}

export interface CounterExampleItem {
  id: number;
  input: string;
  expectedOutput?: string;
  reason: string;
  relatedConstraint?: string;
  createdAt: string;
}

export interface CounterExamples {
  submissionId: number;
  items: CounterExampleItem[];
}
