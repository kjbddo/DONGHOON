package com.algoforge.backend.judge.message;

/**
 * Backend → Judge Worker 메시지.
 * judge-worker 모듈의 동일 이름 record와 필드 호환되어야 한다.
 */
public record JudgeRequestMessage(
        Long submissionId,
        Long problemId,
        String languageName,
        String code,
        Integer timeLimitMs,
        Integer memoryLimitMb
) {}
