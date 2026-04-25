package com.algoforge.backend.ai.dto;

import java.util.Map;

/**
 * AI 서버 POST /ai/problems/generate 요청.
 *
 * AI 서버 측 카테고리/난이도 enum과 정확히 매칭되어야 한다:
 *   category   : DP/GRAPH/GREEDY/STRING/MATH/DS/BFS/DFS/BINARY_SEARCH/...
 *   difficulty : Bronze/Silver/Gold/Platinum/Diamond/Ruby
 */
public record GenerateProblemAiRequest(
        String category,
        String difficulty,
        String topicHint,
        Map<String, Object> sourceMetadata
) {}
