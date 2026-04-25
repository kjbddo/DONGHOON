package com.algoforge.backend.user.controller;

import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.user.dto.RankingEntryResponse;
import com.algoforge.backend.user.service.RankingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 랭킹은 공개 정보로 제공한다 (인증 불필요).
 */
@Tag(name = "Ranking", description = "사용자 랭킹 (푼 문제 수 기준)")
@SecurityRequirements
@RestController
@RequestMapping("/api/users/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ApiResponse<Page<RankingEntryResponse>> ranking(
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(rankingService.getRanking(pageable));
    }
}
