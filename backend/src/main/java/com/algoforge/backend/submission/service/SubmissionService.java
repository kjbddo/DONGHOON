package com.algoforge.backend.submission.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.judge.message.JudgeRequestMessage;
import com.algoforge.backend.judge.producer.JudgeMessageProducer;
import com.algoforge.backend.language.domain.CodeLanguage;
import com.algoforge.backend.language.repository.CodeLanguageRepository;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.submission.domain.Submission;
import com.algoforge.backend.submission.domain.SubmissionTestCaseResult;
import com.algoforge.backend.submission.dto.SubmissionDetailResponse;
import com.algoforge.backend.submission.dto.SubmissionSummaryResponse;
import com.algoforge.backend.submission.dto.SubmitCodeRequest;
import com.algoforge.backend.submission.repository.SubmissionRepository;
import com.algoforge.backend.submission.repository.SubmissionTestCaseResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionTestCaseResultRepository testCaseResultRepository;
    private final ProblemRepository problemRepository;
    private final CodeLanguageRepository languageRepository;
    private final JudgeMessageProducer judgeMessageProducer;

    @Transactional
    public SubmissionSummaryResponse submit(Long userId, SubmitCodeRequest req) {
        Problem problem = problemRepository.findById(req.problemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        if (!problem.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLIC);
        }

        CodeLanguage language = languageRepository.findByName(req.language().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LANGUAGE));

        Submission submission = Submission.builder()
                .userId(userId)
                .problemId(problem.getId())
                .languageId(language.getId())
                .code(req.code())
                .build();
        Submission saved = submissionRepository.save(submission);

        // 트랜잭션 커밋 후 채점 큐로 발행 (DB 미반영 상태에서 워커가 select하는 사고 방지)
        JudgeRequestMessage message = new JudgeRequestMessage(
                saved.getId(),
                problem.getId(),
                language.getName(),
                saved.getCode(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb()
        );
        registerAfterCommit(() -> judgeMessageProducer.publish(message));

        log.info("[SubmissionService] 제출 생성: id={} userId={} problemId={} lang={}",
                saved.getId(), userId, problem.getId(), language.getName());

        return SubmissionSummaryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionSummaryResponse> listMine(Long userId, Long problemIdOrNull, Pageable pageable) {
        Page<Submission> page = (problemIdOrNull == null)
                ? submissionRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : submissionRepository.findByUserIdAndProblemIdOrderByIdDesc(userId, problemIdOrNull, pageable);
        return page.map(SubmissionSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getDetail(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        if (!submission.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_OWNER);
        }
        List<SubmissionTestCaseResult> results =
                testCaseResultRepository.findBySubmissionIdOrderByIdAsc(submissionId);
        return SubmissionDetailResponse.of(submission, results);
    }

    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
