package com.algoforge.backend.user.controller;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.security.CurrentUser;
import com.algoforge.backend.submission.repository.UserSolvedProblemRepository;
import com.algoforge.backend.user.dto.UserResponse;
import com.algoforge.backend.user.dto.UserStatsResponse;
import com.algoforge.backend.user.service.UserService;
import com.algoforge.backend.user.service.UserStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "User", description = "내 정보 / 통계 / 해결한 문제 목록")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserStatsService userStatsService;
    private final UserSolvedProblemRepository solvedRepository;
    private final ProblemRepository problemRepository;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(UserResponse.from(userService.getById(requireUserId(currentUser))));
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> myStats(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(userStatsService.getStats(requireUserId(currentUser)));
    }

    @GetMapping("/me/solved")
    public ApiResponse<Page<ProblemSummaryResponse>> mySolved(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = requireUserId(currentUser);
        Page<Long> idPage = solvedRepository.findSolvedProblemIdsByUserId(userId, pageable);
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return ApiResponse.ok(new PageImpl<>(List.of(), pageable, idPage.getTotalElements()));
        }

        Map<Long, Problem> byId = new HashMap<>();
        for (Problem p : problemRepository.findByIdIn(ids)) {
            byId.put(p.getId(), p);
        }

        List<ProblemSummaryResponse> items = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(ProblemSummaryResponse::from)
                .toList();

        return ApiResponse.ok(new PageImpl<>(items, pageable, idPage.getTotalElements()));
    }

    /** 다른 사용자 공개 통계 (랭킹 페이지에서 클릭 진입 등) */
    @GetMapping("/{userId}/stats")
    public ApiResponse<UserStatsResponse> userStats(
            @org.springframework.web.bind.annotation.PathVariable Long userId
    ) {
        return ApiResponse.ok(userStatsService.getStats(userId));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.userId();
    }
}
