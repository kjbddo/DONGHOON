package com.algoforge.backend.problem.domain;

/**
 * Problem.examples JSONB의 단일 요소.
 * - 예시 입력/출력 + 선택적 설명.
 */
public record Example(
        String input,
        String output,
        String explanation
) {}
