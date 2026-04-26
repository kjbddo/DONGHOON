package com.algoforge.backend.problem.dto;

/**
 * 로그인한 사용자 시점에서 본 문제의 풀이 상태.
 * SOLVED  : 해당 문제를 한 번이라도 ACCEPTED 한 적 있음 (user_solved_problems 존재)
 * WRONG   : 제출 이력은 있지만 아직 ACCEPTED 한 적 없음 (시도만 함)
 * 비로그인이거나 시도 이력 자체가 없을 때는 응답 필드를 null 로 둔다.
 */
public enum ProblemUserStatus {
    SOLVED,
    WRONG
}
