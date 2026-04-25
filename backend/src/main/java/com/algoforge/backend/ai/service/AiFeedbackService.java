package com.algoforge.backend.ai.service;

import com.algoforge.backend.ai.client.AiClient;
import com.algoforge.backend.ai.domain.AiCallPurpose;
import com.algoforge.backend.ai.domain.AiFeedback;
import com.algoforge.backend.ai.dto.AiFeedbackResponse;
import com.algoforge.backend.ai.dto.FeedbackAiRequest;
import com.algoforge.backend.ai.dto.FeedbackAiResponse;
import com.algoforge.backend.ai.repository.AiFeedbackRepository;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 제출별/레벨별 1회만 AI를 호출하고 결과를 영속화한다.
 * (submission_id, feedback_level) UNIQUE 제약 위에서 캐싱.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private static final String DEFAULT_MODEL = "gemini";
    private static final String DEFAULT_PROMPT_VERSION = "v1";

    private final AiClient aiClient;
    private final AiFeedbackRepository repository;
    private final SubmissionAiContextLoader contextLoader;
    private final AiQuotaService quotaService;
    private final AiCallLogger callLogger;

    public AiFeedbackResponse getOrCreate(Long submissionId, Long requesterUserId, int feedbackLevel) {
        if (feedbackLevel < 1 || feedbackLevel > 4) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "feedbackLevel은 1~4 사이여야 합니다.");
        }

        // 1) 캐시 hit
        var cached = repository.findBySubmissionIdAndFeedbackLevel(submissionId, (short) feedbackLevel);
        if (cached.isPresent()) {
            return AiFeedbackResponse.from(cached.get());
        }

        // 2) 컨텍스트 로드 + 권한/완료 검증
        Context ctx = contextLoader.load(submissionId, requesterUserId);
        Submission submission = ctx.submission();
        if (submission.getStatus() == SubmissionStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답 제출에는 피드백을 제공하지 않습니다.");
        }

        // 3) 일일 quota 검증
        quotaService.assertWithinDailyQuota(requesterUserId);

        // 4) AI 호출 (성공/실패 모두 ai_call_logs에 적재)
        FeedbackAiRequest req = buildRequest(ctx, feedbackLevel);
        FeedbackAiResponse ai = invokeWithLogging(requesterUserId, req);

        // 5) 저장 (UNIQUE 충돌 시 race condition 방지)
        try {
            AiFeedback saved = repository.save(toEntity(submission, ctx.problem(), feedbackLevel, ai));
            return AiFeedbackResponse.from(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            log.info("AI 피드백 중복 저장 감지 - 캐시 재조회: submissionId={} level={}", submissionId, feedbackLevel);
            return AiFeedbackResponse.from(
                    repository.findBySubmissionIdAndFeedbackLevel(submissionId, (short) feedbackLevel)
                            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR))
            );
        }
    }

    @Transactional(readOnly = true)
    public AiFeedbackResponse getCachedOnly(Long submissionId, Long requesterUserId, int feedbackLevel) {
        // 권한 확인용으로만 컨텍스트 로드
        contextLoader.load(submissionId, requesterUserId);
        return repository.findBySubmissionIdAndFeedbackLevel(submissionId, (short) feedbackLevel)
                .map(AiFeedbackResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 레벨 피드백이 아직 없습니다."));
    }

    // ===== private =====
    private FeedbackAiResponse invokeWithLogging(Long userId, FeedbackAiRequest req) {
        long startedAt = System.nanoTime();
        try {
            FeedbackAiResponse res = await(aiClient.generateFeedback(req));
            callLogger.logSuccess(AiCallPurpose.FEEDBACK, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, res, elapsedMs(startedAt));
            return res;
        } catch (RuntimeException ex) {
            callLogger.logFailure(AiCallPurpose.FEEDBACK, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, elapsedMs(startedAt), ex);
            throw ex;
        }
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private FeedbackAiRequest buildRequest(Context ctx, int level) {
        Problem p = ctx.problem();
        Submission s = ctx.submission();
        return new FeedbackAiRequest(
                p.getTitle(),
                p.getDescription(),
                p.getInputDescription(),
                p.getOutputDescription(),
                p.getConstraints(),
                s.getCode(),
                ctx.language().getName(),
                s.getStatus().name(),
                level,
                ctx.failedTestExcerpt(),
                s.getRuntimeErrorMessage(),
                s.getCompileErrorMessage()
        );
    }

    private AiFeedback toEntity(Submission s, Problem p, int level, FeedbackAiResponse ai) {
        return AiFeedback.builder()
                .submissionId(s.getId())
                .userId(s.getUserId())
                .problemId(p.getId())
                .feedbackLevel((short) level)
                .summary(ai.summary())
                .directionHint(ai.directionHint())
                .counterExampleHint(ai.counterExampleHint())
                .complexityHint(ai.complexityHint())
                .runtimeErrorHint(ai.runtimeErrorHint())
                .compileErrorHint(ai.compileErrorHint())
                .modelName(DEFAULT_MODEL)
                .promptVersion(DEFAULT_PROMPT_VERSION)
                .build();
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
            log.warn("AI 피드백 호출 실패", cause);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 호출 실패: " + cause.getMessage());
        }
    }
}
