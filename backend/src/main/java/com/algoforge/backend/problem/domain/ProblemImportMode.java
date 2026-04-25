package com.algoforge.backend.problem.domain;

/**
 * 관리자 JSON 가져오기 흐름. 운영에서 라이선스/출처 URL 정책을 분기하는 데 사용.
 */
public enum ProblemImportMode {
    /** JSON 메타데이터만으로 DRAFT 생성 (sourceType: ADMIN) */
    METADATA_ONLY,
    /** URL·라이선스 확인이 있는 경우. sourceType: LICENSED_IMPORTED */
    LICENSED_IMPORT,
    /**
     * 외부·원문 기반을 편집/재서술한 문제로 표기 (sourceType: AI_REWRITTEN_SOURCE_BASED).
     * (실제 LLM 호출은 별도 파이프라인; 여기서는 DRAFT 본문을 메타로 채움)
     */
    AI_REWRITE_FROM_METADATA
}
