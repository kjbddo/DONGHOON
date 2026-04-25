package com.algoforge.backend.problem.dto;

import com.algoforge.backend.problem.domain.Example;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemCategory;
import com.algoforge.backend.problem.domain.ProblemTag;
import com.algoforge.backend.problem.domain.TestCase;

import java.util.List;

/**
 * 일반 사용자에게 노출되는 문제 상세.
 * - 솔루션 코드/해설은 제외 (관리자 전용)
 * - hidden 테스트 케이스도 제외 (예시 입출력만 보여줌)
 */
public record ProblemDetailResponse(
        Long id,
        String slug,
        String title,
        String description,
        String inputDescription,
        String outputDescription,
        List<String> constraints,
        List<Example> examples,
        int timeLimitMs,
        int memoryLimitMb,
        String difficulty,
        boolean aiGenerated,
        List<String> categories,
        List<String> tags,
        List<PublicTestCaseDto> publicTestCases
) {
    public static ProblemDetailResponse of(Problem p, List<TestCase> publicTestCases) {
        return new ProblemDetailResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getDescription(),
                p.getInputDescription(),
                p.getOutputDescription(),
                p.getConstraints(),
                p.getExamples(),
                p.getTimeLimitMs(),
                p.getMemoryLimitMb(),
                p.getDifficulty().name(),
                p.isAiGenerated(),
                p.getCategories().stream().map(ProblemCategory::getName).toList(),
                p.getTags().stream().map(ProblemTag::getName).toList(),
                publicTestCases.stream().map(PublicTestCaseDto::from).toList()
        );
    }

    public record PublicTestCaseDto(int seq, String input, String expectedOutput) {
        public static PublicTestCaseDto from(TestCase tc) {
            return new PublicTestCaseDto(tc.getSeq(), tc.getInput(), tc.getExpectedOutput());
        }
    }
}
