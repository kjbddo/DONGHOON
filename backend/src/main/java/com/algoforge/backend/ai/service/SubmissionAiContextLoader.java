package com.algoforge.backend.ai.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.language.domain.CodeLanguage;
import com.algoforge.backend.language.repository.CodeLanguageRepository;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.TestCase;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.problem.repository.TestCaseRepository;
import com.algoforge.backend.submission.domain.Submission;
import com.algoforge.backend.submission.domain.SubmissionStatus;
import com.algoforge.backend.submission.domain.SubmissionTestCaseResult;
import com.algoforge.backend.submission.repository.SubmissionRepository;
import com.algoforge.backend.submission.repository.SubmissionTestCaseResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 제출 ID에 대해 AI 호출 시 필요한 컨텍스트(제출 + 문제 + 언어 + 실패한 테스트 발췌)를 모아준다.
 */
@Component
@RequiredArgsConstructor
public class SubmissionAiContextLoader {

    private static final int MAX_EXCERPT_LEN = 600;

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final CodeLanguageRepository languageRepository;
    private final SubmissionTestCaseResultRepository testCaseResultRepository;
    private final TestCaseRepository testCaseRepository;

    public record Context(
            Submission submission,
            Problem problem,
            CodeLanguage language,
            String failedTestExcerpt
    ) {}

    @Transactional(readOnly = true)
    public Context load(Long submissionId, Long requesterUserId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (!submission.getUserId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_OWNER);
        }
        if (!submission.getStatus().isFinal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "채점이 완료되지 않은 제출입니다.");
        }

        Problem problem = problemRepository.findWithRelationsById(submission.getProblemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        CodeLanguage language = languageRepository.findById(submission.getLanguageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LANGUAGE));

        String excerpt = buildFailedTestExcerpt(submission);
        return new Context(submission, problem, language, excerpt);
    }

    /**
     * 실패한 테스트 케이스 중 하나를 골라 입력/기대출력/실제출력 일부를 직렬화한다.
     * 정답인 경우 null.
     */
    private String buildFailedTestExcerpt(Submission submission) {
        if (submission.getStatus() == SubmissionStatus.ACCEPTED) return null;
        List<SubmissionTestCaseResult> results = testCaseResultRepository
                .findBySubmissionIdOrderByIdAsc(submission.getId());
        if (results.isEmpty()) return null;

        SubmissionTestCaseResult failed = results.stream()
                .filter(r -> r.getStatus() != SubmissionStatus.ACCEPTED)
                .findFirst()
                .orElse(null);
        if (failed == null) return null;

        Map<Long, TestCase> tcMap = new HashMap<>();
        testCaseRepository.findByProblem_IdOrderBySeqAsc(submission.getProblemId())
                .forEach(tc -> tcMap.put(tc.getId(), tc));
        TestCase tc = tcMap.get(failed.getTestCaseId());

        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(failed.getStatus()).append('\n');
        if (tc != null) {
            sb.append("input=\n").append(truncate(tc.getInput())).append('\n');
            sb.append("expected=\n").append(truncate(tc.getExpectedOutput())).append('\n');
        }
        if (failed.getOutputExcerpt() != null) {
            sb.append("actual=\n").append(truncate(failed.getOutputExcerpt())).append('\n');
        }
        return sb.toString();
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > MAX_EXCERPT_LEN ? s.substring(0, MAX_EXCERPT_LEN) + "..." : s;
    }
}
