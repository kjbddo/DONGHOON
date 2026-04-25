package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.Example;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemCategory;
import com.algoforge.backend.problem.domain.ProblemTag;
import com.algoforge.backend.problem.domain.TestCase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 관리자 전용 문제 상세 응답.
 * - hidden 테스트 케이스 포함
 * - 상태/소스타입/AI 메타정보 포함
 * - (TODO) 솔루션 코드는 추후 Solution 도메인 추가 시 함께 노출
 */
public record AdminProblemDetailResponse(
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
        String status,
        String sourceType,
        boolean aiGenerated,
        String aiModelName,
        String aiPromptVersion,
        Long generatedByUserId,
        BigDecimal qualityScore,
        int reportCount,
        OffsetDateTime generatedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> categories,
        List<String> tags,
        List<TestCaseDetail> testCases
) {
    public static AdminProblemDetailResponse of(Problem p, List<TestCase> testCases) {
        return new AdminProblemDetailResponse(
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
                p.getStatus().name(),
                p.getSourceType().name(),
                p.isAiGenerated(),
                p.getAiModelName(),
                p.getAiPromptVersion(),
                p.getGeneratedByUserId(),
                p.getQualityScore(),
                p.getReportCount(),
                p.getGeneratedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getCategories().stream().map(ProblemCategory::getName).toList(),
                p.getTags().stream().map(ProblemTag::getName).toList(),
                testCases.stream().map(TestCaseDetail::from).toList()
        );
    }

    public record TestCaseDetail(Long id, int seq, String input, String expectedOutput, boolean hidden) {
        public static TestCaseDetail from(TestCase tc) {
            return new TestCaseDetail(tc.getId(), tc.getSeq(), tc.getInput(), tc.getExpectedOutput(), tc.isHidden());
        }
    }
}
