package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Example;
import com.algoforge.backend.problem.domain.ProblemSourceType;

import java.util.List;

/**
 * AI 응답으로 문제를 생성할 때 내부에서 사용하는 명령 객체.
 * - REST 입력이 아닌 백엔드 내부에서만 사용 (검증 어노테이션 없음)
 * - AI 메타데이터(modelName, promptVersion, generatedByUserId, sourceType) 포함
 */
public record AiProblemCreateCommand(
        String title,
        String slug,
        String description,
        String inputDescription,
        String outputDescription,
        List<String> constraints,
        List<Example> examples,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Difficulty difficulty,
        ProblemSourceType sourceType,
        List<String> categories,
        List<String> tags,
        List<TestCaseDto> testCases,
        String aiModelName,
        String aiPromptVersion,
        Long generatedByUserId
) {}
