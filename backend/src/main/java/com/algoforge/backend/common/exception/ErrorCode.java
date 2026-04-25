package com.algoforge.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // ===== Common =====
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "충돌이 발생했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류"),

    // ===== Auth =====
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    USER_SUSPENDED(HttpStatus.LOCKED, "이용이 제한된 계정입니다."),

    // ===== Problem =====
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),
    PROBLEM_NOT_PUBLIC(HttpStatus.CONFLICT, "공개 상태가 아닌 문제입니다."),

    // ===== Submission =====
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "제출을 찾을 수 없습니다."),
    SUBMISSION_NOT_OWNER(HttpStatus.FORBIDDEN, "해당 제출의 소유자가 아닙니다."),
    INVALID_LANGUAGE(HttpStatus.BAD_REQUEST, "지원하지 않는 언어입니다."),
    SUBMIT_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),

    // ===== AI =====
    AI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 사용량 한도를 초과했습니다."),
    AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI 응답 생성에 실패했습니다."),

    // ===== Import =====
    IMPORT_BLOCKED_BY_POLICY(HttpStatus.FORBIDDEN, "정책상 가져올 수 없는 문제입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
