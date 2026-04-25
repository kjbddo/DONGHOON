package com.algoforge.judge.meta;

public record ProblemMeta(
        Long id,
        int timeLimitMs,
        int memoryLimitMb
) {}
