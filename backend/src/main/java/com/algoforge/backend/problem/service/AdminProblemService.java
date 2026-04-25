package com.algoforge.backend.problem.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemCategory;
import com.algoforge.backend.problem.domain.ProblemSourceType;
import com.algoforge.backend.problem.domain.ProblemStatus;
import com.algoforge.backend.problem.domain.ProblemTag;
import com.algoforge.backend.problem.domain.TestCase;
import com.algoforge.backend.problem.dto.admin.AdminProblemCreateRequest;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemSummaryResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemUpdateRequest;
import com.algoforge.backend.problem.dto.admin.AiProblemCreateCommand;
import com.algoforge.backend.problem.dto.admin.TestCaseDto;
import com.algoforge.backend.problem.repository.ProblemCategoryRepository;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.problem.repository.ProblemTagRepository;
import com.algoforge.backend.problem.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemCategoryRepository categoryRepository;
    private final ProblemTagRepository tagRepository;
    private final TestCaseRepository testCaseRepository;

    // ===== 목록/상세 =====
    @Transactional(readOnly = true)
    public Page<AdminProblemSummaryResponse> list(
            ProblemStatus status,
            Difficulty difficulty,
            Boolean aiOnly,
            boolean includeDeleted,
            String keyword,
            Pageable pageable
    ) {
        return problemRepository
                .searchAdmin(status, difficulty, aiOnly, includeDeleted, blankToNull(keyword), pageable)
                .map(AdminProblemSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminProblemDetailResponse get(Long id) {
        Problem problem = problemRepository.findWithRelationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        List<TestCase> testCases = testCaseRepository.findByProblem_IdOrderBySeqAsc(id);
        return AdminProblemDetailResponse.of(problem, testCases);
    }

    // ===== 생성 =====
    public AdminProblemDetailResponse create(AdminProblemCreateRequest req) {
        String slug = resolveSlug(req.slug(), req.title());

        Problem problem = Problem.builder()
                .title(req.title())
                .slug(slug)
                .description(req.description())
                .inputDescription(req.inputDescription())
                .outputDescription(req.outputDescription())
                .constraints(req.constraints())
                .examples(req.examples())
                .timeLimitMs(req.timeLimitMs() == null ? 2000 : req.timeLimitMs())
                .memoryLimitMb(req.memoryLimitMb() == null ? 256 : req.memoryLimitMb())
                .difficulty(req.difficulty())
                .status(ProblemStatus.DRAFT)
                .sourceType(req.sourceType() == null ? ProblemSourceType.ADMIN_CREATED : req.sourceType())
                .aiGenerated(false)
                .build();

        applyCategoriesAndTags(problem, req.categories(), req.tags());
        Problem saved = problemRepository.save(problem);

        replaceTestCases(saved, req.testCases());

        log.info("관리자 문제 생성: id={} slug={}", saved.getId(), saved.getSlug());
        return get(saved.getId());
    }

    // ===== 생성: AI =====
    public AdminProblemDetailResponse createFromAi(AiProblemCreateCommand cmd) {
        String slug = resolveSlug(cmd.slug(), cmd.title());

        Problem problem = Problem.builder()
                .title(cmd.title())
                .slug(slug)
                .description(cmd.description())
                .inputDescription(cmd.inputDescription())
                .outputDescription(cmd.outputDescription())
                .constraints(cmd.constraints())
                .examples(cmd.examples())
                .timeLimitMs(cmd.timeLimitMs() == null ? 2000 : cmd.timeLimitMs())
                .memoryLimitMb(cmd.memoryLimitMb() == null ? 256 : cmd.memoryLimitMb())
                .difficulty(cmd.difficulty())
                .status(ProblemStatus.DRAFT)               // AI 생성은 항상 검토 대기 상태로
                .sourceType(cmd.sourceType() == null ? ProblemSourceType.AI_GENERATED : cmd.sourceType())
                .aiGenerated(true)
                .aiModelName(cmd.aiModelName())
                .aiPromptVersion(cmd.aiPromptVersion())
                .generatedByUserId(cmd.generatedByUserId())
                .build();

        applyCategoriesAndTags(problem, cmd.categories(), cmd.tags());
        Problem saved = problemRepository.save(problem);

        replaceTestCases(saved, cmd.testCases());

        log.info("AI 문제 생성: id={} slug={} model={}", saved.getId(), saved.getSlug(), cmd.aiModelName());
        return get(saved.getId());
    }

    // ===== 수정 =====
    public AdminProblemDetailResponse update(Long id, AdminProblemUpdateRequest req) {
        Problem problem = problemRepository.findWithRelationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        problem.update(
                req.title(),
                req.description(),
                req.inputDescription(),
                req.outputDescription(),
                req.constraints(),
                req.examples(),
                req.timeLimitMs() == null ? 0 : req.timeLimitMs(),
                req.memoryLimitMb() == null ? 0 : req.memoryLimitMb(),
                req.difficulty()
        );

        applyCategoriesAndTags(problem, req.categories(), req.tags());

        if (req.testCases() != null) {
            replaceTestCases(problem, req.testCases());
        }

        log.info("관리자 문제 수정: id={}", problem.getId());
        return get(problem.getId());
    }

    // ===== 상태 변경 =====
    public AdminProblemDetailResponse changeStatus(Long id, ProblemStatus next) {
        Problem problem = problemRepository.findWithRelationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        problem.changeStatus(next);
        return get(problem.getId());
    }

    // ===== Soft Delete =====
    public void delete(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        problem.softDelete();
    }

    // ===== private helpers =====
    private void applyCategoriesAndTags(Problem problem, List<String> categoryNames, List<String> tagNames) {
        if (categoryNames != null) {
            problem.replaceCategories(upsertCategories(categoryNames));
        }
        if (tagNames != null) {
            problem.replaceTags(upsertTags(tagNames));
        }
    }

    private Set<ProblemCategory> upsertCategories(List<String> names) {
        List<String> normalized = names.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (normalized.isEmpty()) return new HashSet<>();
        Map<String, ProblemCategory> existing = categoryRepository.findByNameIn(normalized).stream()
                .collect(Collectors.toMap(ProblemCategory::getName, c -> c));
        Set<ProblemCategory> result = new HashSet<>(existing.values());
        for (String name : normalized) {
            if (!existing.containsKey(name)) {
                result.add(categoryRepository.save(new ProblemCategory(name)));
            }
        }
        return result;
    }

    private Set<ProblemTag> upsertTags(List<String> names) {
        List<String> normalized = names.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (normalized.isEmpty()) return new HashSet<>();
        Map<String, ProblemTag> existing = tagRepository.findByNameIn(normalized).stream()
                .collect(Collectors.toMap(ProblemTag::getName, t -> t));
        Set<ProblemTag> result = new HashSet<>(existing.values());
        for (String name : normalized) {
            if (!existing.containsKey(name)) {
                result.add(tagRepository.save(new ProblemTag(name)));
            }
        }
        return result;
    }

    private void replaceTestCases(Problem problem, List<TestCaseDto> testCases) {
        testCaseRepository.deleteAllByProblemId(problem.getId());
        if (testCases == null || testCases.isEmpty()) return;
        AtomicInteger seqCounter = new AtomicInteger(1);
        List<TestCase> entities = testCases.stream()
                .map(t -> TestCase.builder()
                        .problem(problem)
                        .input(t.input())
                        .expectedOutput(t.expectedOutput())
                        .hidden(t.hidden())
                        .seq(t.seq() == null ? seqCounter.getAndIncrement() : t.seq())
                        .build())
                .toList();
        testCaseRepository.saveAll(entities);
    }

    private String resolveSlug(String requested, String title) {
        String base = blankToNull(requested);
        if (base == null) {
            base = slugifyTitle(title);
        }
        if (!problemRepository.existsBySlug(base)) {
            return base;
        }
        // 충돌 시 -2, -3 ... 자동 접미사
        for (int i = 2; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (!problemRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "slug 자동 생성 실패");
    }

    private String slugifyTitle(String title) {
        if (title == null || title.isBlank()) return "problem-" + System.currentTimeMillis();
        String n = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = n.toLowerCase()
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > 200) slug = slug.substring(0, 200);
        if (slug.isBlank()) slug = "problem-" + System.currentTimeMillis();
        return slug;
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * (참고) 카테고리/태그를 미리 캐싱해 두고 싶을 때 사용 가능한 헬퍼.
     * 현재는 단순 구현으로 호출 시 매번 DB 조회.
     */
    @SuppressWarnings("unused")
    private Map<String, Long> emptyCache() {
        return new HashMap<>();
    }
}
