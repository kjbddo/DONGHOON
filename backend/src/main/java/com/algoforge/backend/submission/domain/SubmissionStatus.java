package com.algoforge.backend.submission.domain;

public enum SubmissionStatus {
    PENDING,
    JUDGING,
    ACCEPTED,
    WRONG_ANSWER,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    SYSTEM_ERROR;

    public boolean isFinal() {
        return this != PENDING && this != JUDGING;
    }

    public boolean isAccepted() {
        return this == ACCEPTED;
    }

    public static SubmissionStatus fromExternal(String value) {
        if (value == null) return SYSTEM_ERROR;
        return switch (value.toUpperCase()) {
            case "ACCEPTED" -> ACCEPTED;
            case "WRONG_ANSWER" -> WRONG_ANSWER;
            case "COMPILE_ERROR" -> COMPILE_ERROR;
            case "RUNTIME_ERROR" -> RUNTIME_ERROR;
            case "TLE", "TIME_LIMIT_EXCEEDED" -> TIME_LIMIT_EXCEEDED;
            case "MLE", "MEMORY_LIMIT_EXCEEDED" -> MEMORY_LIMIT_EXCEEDED;
            case "PENDING" -> PENDING;
            case "JUDGING" -> JUDGING;
            default -> SYSTEM_ERROR;
        };
    }
}
