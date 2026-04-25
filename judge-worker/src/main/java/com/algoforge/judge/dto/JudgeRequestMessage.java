package com.algoforge.judge.dto;

public record JudgeRequestMessage(
        Long submissionId,
        Long problemId,
        String languageName,
        String code,
        Integer timeLimitMs,
        Integer memoryLimitMb
) {}
