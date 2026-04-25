package com.algoforge.backend.judge.consumer;

import com.algoforge.backend.judge.message.JudgeResultMessage;
import com.algoforge.backend.submission.domain.Submission;
import com.algoforge.backend.submission.domain.SubmissionStatus;
import com.algoforge.backend.submission.domain.SubmissionTestCaseResult;
import com.algoforge.backend.submission.domain.UserSolvedProblem;
import com.algoforge.backend.submission.repository.SubmissionRepository;
import com.algoforge.backend.submission.repository.SubmissionTestCaseResultRepository;
import com.algoforge.backend.submission.repository.UserSolvedProblemRepository;
import com.algoforge.backend.submission.sse.SubmissionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * judge.result 큐를 구독하여:
 * 1) Submission 상태/시간/메모리/에러 메시지 업데이트
 * 2) 테스트 케이스별 결과 저장 (덮어쓰기)
 * 3) ACCEPTED일 때 UserSolvedProblem(첫 정답) 기록
 * 4) SSE로 실시간 알림 푸시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeResultConsumer {

    private final SubmissionRepository submissionRepository;
    private final SubmissionTestCaseResultRepository testCaseResultRepository;
    private final UserSolvedProblemRepository userSolvedProblemRepository;
    private final SubmissionEventPublisher eventPublisher;

    @RabbitListener(queues = "${algoforge.judge.result-queue}")
    @Transactional
    public void onResult(JudgeResultMessage msg) {
        if (msg == null || msg.submissionId() == null) {
            log.warn("[JudgeResultConsumer] 잘못된 메시지 수신, 무시");
            return;
        }
        Long submissionId = msg.submissionId();
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("[JudgeResultConsumer] submission 없음: id={}", submissionId);
            return;
        }

        SubmissionStatus next = SubmissionStatus.fromExternal(msg.status());
        submission.applyResult(
                next,
                msg.maxExecutionTimeMs(),
                msg.maxMemoryUsedKb(),
                msg.compileErrorMessage(),
                msg.runtimeErrorMessage()
        );

        // 테스트 케이스 결과는 멱등성을 위해 전체 교체.
        testCaseResultRepository.deleteAllBySubmissionId(submissionId);
        if (msg.testCaseResults() != null && !msg.testCaseResults().isEmpty()) {
            List<SubmissionTestCaseResult> entities = msg.testCaseResults().stream()
                    .map(t -> SubmissionTestCaseResult.builder()
                            .submissionId(submissionId)
                            .testCaseId(t.testCaseId())
                            .status(SubmissionStatus.fromExternal(t.status()))
                            .executionTimeMs(t.executionTimeMs())
                            .memoryUsedKb(t.memoryUsedKb())
                            .outputExcerpt(t.outputExcerpt())
                            .build())
                    .toList();
            testCaseResultRepository.saveAll(entities);
        }

        // 첫 정답 기록 (idempotent)
        if (next.isAccepted()) {
            UserSolvedProblem.Pk pk = new UserSolvedProblem.Pk(submission.getUserId(), submission.getProblemId());
            if (!userSolvedProblemRepository.existsById(pk)) {
                userSolvedProblemRepository.save(
                        new UserSolvedProblem(submission.getUserId(), submission.getProblemId(), submission.getId(), 1)
                );
            }
        }

        // SSE 푸시
        eventPublisher.publish(submissionId, "result", new ResultEventDto(
                submissionId,
                next.name(),
                msg.maxExecutionTimeMs(),
                msg.maxMemoryUsedKb()
        ));
        if (next.isFinal()) {
            eventPublisher.complete(submissionId);
        }

        log.info("[JudgeResultConsumer] 채점 결과 반영: submissionId={} status={}", submissionId, next);
    }

    public record ResultEventDto(Long submissionId, String status,
                                 Integer executionTimeMs, Integer memoryUsedKb) {}
}
