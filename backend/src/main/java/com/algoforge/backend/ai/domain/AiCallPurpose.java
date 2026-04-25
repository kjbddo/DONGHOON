package com.algoforge.backend.ai.domain;

/**
 * ai_call_logs.purpose 컬럼 값과 1:1 대응.
 * 마이그레이션 V1에서 정의된 VARCHAR(50)에 들어가는 도메인 값이므로
 * 새 값 추가 시 데이터 마이그레이션이 필요 없도록 enum.name() 그대로 저장한다.
 */
public enum AiCallPurpose {
    PROBLEM_GEN,
    FEEDBACK,
    COUNTER_EXAMPLE
}
