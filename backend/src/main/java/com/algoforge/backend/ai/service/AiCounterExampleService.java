package com.algoforge.backend.ai.service;

import com.algoforge.backend.ai.client.AiClient;
import com.algoforge.backend.ai.domain.AiCallPurpose;
import com.algoforge.backend.ai.domain.CounterExample;
import com.algoforge.backend.ai.dto.CounterExampleAiRequest;
import com.algoforge.backend.ai.dto.CounterExampleAiResponse;
import com.algoforge.backend.ai.dto.CounterExampleResponse;
import com.algoforge.backend.ai.repository.CounterExampleRepository;
import com.algoforge.backend.ai.service.SubmissionAiContextLoader.Context;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.submission.domain.Submission;
import com.algoforge.backend.submission.domain.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 제출 단위 반례 생성/조회 서비스.
 *
 * 정책:
 * - 정답(ACCEPTED) 제출에는 반례를 만들지 않는다.
 * - 같은 제출에 대해 이미 반례가 1건이라도 저장되어 있으면 캐시 반환.
 *   (피드백처럼 레벨 단위가 아니라 1회만 호출)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCounterExampleService {

    private static final String DEFAULT_MODEL = "gemini";
    private static final String DEFAULT_PROMPT_VERSION = "v1";

    private final AiClient aiClient;
    private final CounterExampleRepository repository;
    private final SubmissionAiContextLoader contextLoader;
    private final AiQuotaService quotaService;
    private final AiCallLogger callLogger;

    public CounterExampleResponse getOrCreate(Long submissionId, Long requesterUserId) {
        if (repository.countBySubmissionId(submissionId) > 0) {
            // 권한만 한 번 검증 후 캐시 반환
            contextLoader.load(submissionId, requesterUserId);
            return cached(submissionId);
        }

        Context ctx = contextLoader.load(submissionId, requesterUserId);
        Submission submission = ctx.submission();
        if (submission.getStatus() == SubmissionStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답 제출에는 반례를 생성하지 않습니다.");
        }

        // 일일 quota 검증
        quotaService.assertWithinDailyQuota(requesterUserId);

        CounterExampleAiRequest req = buildRequest(ctx);
        CounterExampleAiResponse ai = invokeWithLogging(requesterUserId, req);
        if (ai.counterExamples() == null || ai.counterExamples().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 응답에 반례가 없습니다.");
        }

        List<CounterExample> entities = ai.counterExamples().stream()
                .map(item -> CounterExample.builder()
                        .submissionId(submission.getId())
                        .problemId(submission.getProblemId())
                        .userId(submission.getUserId())
                        .inputData(item.input())
                        .expectedOutput(item.expectedOutput())
                        .reason(item.reason())
                        .relatedConstraint(item.relatedConstraint())
                        .source("AI")
                        .build())
                .toList();
        repository.saveAll(entities);

        return cached(submissionId);
    }

    @Transactional(readOnly = true)
    public CounterExampleResponse getCachedOnly(Long submissionId, Long requesterUserId) {
        contextLoader.load(submissionId, requesterUserId); // 권한 검증
        List<CounterExample> items = repository.findAllBySubmissionIdOrderByIdAsc(submissionId);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이 제출에 대한 반례가 아직 없습니다.");
        }
        return CounterExampleResponse.of(submissionId, items);
    }

    // ===== private =====
    private CounterExampleAiResponse invokeWithLogging(Long userId, CounterExampleAiRequest req) {
        long startedAt = System.nanoTime();
        try {
            CounterExampleAiResponse res = await(aiClient.generateCounterExamples(req));
            callLogger.logSuccess(AiCallPurpose.COUNTER_EXAMPLE, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, res, elapsedMs(startedAt));
            return res;
        } catch (RuntimeException ex) {
            callLogger.logFailure(AiCallPurpose.COUNTER_EXAMPLE, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, elapsedMs(startedAt), ex);
            throw ex;
        }
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private CounterExampleResponse cached(Long submissionId) {
        return CounterExampleResponse.of(
                submissionId,
                repository.findAllBySubmissionIdOrderByIdAsc(submissionId)
        );
    }

    private CounterExampleAiRequest buildRequest(Context ctx) {
        Problem p = ctx.problem();
        Submission s = ctx.submission();
        return new CounterExampleAiRequest(
                p.getTitle(),
                p.getDescription(),
                p.getConstraints(),
                s.getCode(),
                ctx.language().getName(),
                ctx.failedTestExcerpt()
        );
    }

    private <T> T await(CompletableFuture<T> f) {
        try {
            return f.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 호출이 중단되었습니다.");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof BusinessException be) throw be;
            log.warn("AI 반례 호출 실패", cause);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 호출 실패: " + cause.getMessage());
        }
    }
}
